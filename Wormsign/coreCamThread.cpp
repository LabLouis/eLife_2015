#include "coreCamThread.h"
#include "wormsign.h"
#include "experimentAgent.h"
#include "analysisModule.h"



CoreCamThread::CoreCamThread(int cId, Wormsign * ui):QThread(){
	
	umPerPixel = 7.62; //calibration factor for camera pixels to microns

	exited = true;
	stopped = true;
	pause = false; paused = false;
		
	gui = ui;

	camId = cId;
	
	//Pylon API interface to connect to the only connected camera
	CTlFactory & TlFactory = CTlFactory::GetInstance();
	pTl = TlFactory.CreateTl( Basler1394DeviceClass );

	DeviceInfoList_t lstDevices;
	pTl->EnumerateDevices(lstDevices);

	IPylonDevice * dev = CTlFactory::GetInstance().CreateDevice(lstDevices[camId]);

	camera.Attach(dev);
	camera.Open();

	camera.PixelFormat.SetValue(PixelFormat_Mono8);
	camera.TriggerSource.SetValue(Basler_IIDC1394CameraParams::TriggerSource_Line1);
	camera.TriggerActivation.SetValue(Basler_IIDC1394CameraParams::TriggerActivation_RisingEdge);
	camera.TriggerMode.SetValue(Basler_IIDC1394CameraParams::TriggerMode_Off);
	camera.ExposureMode.SetValue(Basler_IIDC1394CameraParams::ExposureMode_Timed);

	camera.PacketSize.SetValue(4096);
	
	//turn off trigger initially for previewing
	expActive = false;
	
	
	updateRoi();


};

CoreCamThread::~CoreCamThread(){
	camera.Close();
	CTlFactory & TlFactory = CTlFactory::GetInstance();
	TlFactory.ReleaseTl(pTl);
};

QStringList CoreCamThread::listCams(){
	QStringList availableCameras;

	//PylonAutoInitTerm autoInitTerm;
	CTlFactory & TlFactory = CTlFactory::GetInstance();
	ITransportLayer* pTl = TlFactory.CreateTl( Pylon::Basler1394DeviceClass );

	DeviceInfoList_t lstDevices;
	pTl->EnumerateDevices(lstDevices);

	DeviceInfoList_t::const_iterator it;
	int ncams = lstDevices.size();
	for(it = lstDevices.begin(); it != lstDevices.end(); it++){
		availableCameras << QString().sprintf("%s - %s",it->GetFullName().c_str(),it->GetSerialNumber().c_str());
	}

	return availableCameras;
};

//turn on the hardware trigger line
void CoreCamThread::setTrig(bool state){
	if(!stopped) return;
	
	if(state){
		camera.TriggerMode.SetValue(TriggerMode_On);
	}else{
		camera.TriggerMode.SetValue(TriggerMode_Off);
	}

	expActive = state;
};

void CoreCamThread::run(){

	//Maximize thread priority relative to process priority
	SetThreadPriority(GetCurrentThread(),THREAD_PRIORITY_TIME_CRITICAL);

	//Profile Variables
	LARGE_INTEGER pcTic,pcToc,pcFreq;
	LARGE_INTEGER pcATic, pcAToc;
	QueryPerformanceFrequency(&pcFreq);

	//Image Buffers
	frameCount = -1;
	imBuffer.resize(nBuffers);
	uchar * im;

	//create pylon buffers
	
	//stream grabber
	int numGrabbers = camera.GetNumStreamGrabberChannels();
	CBasler1394Camera::StreamGrabber_t stream;
	IStreamGrabber* pGrabber = camera.GetStreamGrabber(0);
	stream.Attach(pGrabber);
	
	//connect a stream grabber
	stream.Open();
	stream.MaxBufferSize = camera.PayloadSize;
	stream.MaxNumBuffer = nBuffers;

	stream.PrepareGrab();
		
	//Load buffers into stream queue
	std::vector<unsigned char*> ppBuffers; ppBuffers.resize(nBuffers);
	std::vector<StreamBufferHandle> handles; handles.resize(nBuffers);

	for (int j=0; j<nBuffers; ++j){
		ppBuffers[j] = new unsigned char[(int)camera.PayloadSize.GetValue()];
		handles[j] = stream.RegisterBuffer(ppBuffers[j],(int)camera.PayloadSize.GetValue());
		stream.QueueBuffer(handles[j]);
	}

	
	camera.AcquisitionMode.SetValue(AcquisitionMode_Continuous);
	camera.AcquisitionStart.Execute();

	QueryPerformanceCounter(&pcTic);

	exited = false;
	stopped = false;
	pause = false; paused = false;
	
	std::vector<GrabResult> Result; Result.resize(nBuffers);
	QueryPerformanceCounter(&pcToc);
		
	while(!stopped){

		//for synchronizing
		if (pause) paused = true;
		else paused = false;
		if (paused) { Sleep(1); continue;}

	
		if (stream.GetWaitObject().Wait(3000)){
			stream.RetrieveResult( Result[(frameCount+1)%nBuffers] );
			if (Result[(frameCount+1)%nBuffers].Succeeded()){
				im = (uchar*)Result[(frameCount+1)%nBuffers].Buffer();
			}else{
				
			}
			
			if((frameCount-20)>=0){
				int qInd = (frameCount-20)%nBuffers;
				stream.QueueBuffer(Result[qInd].Handle());
			}
		}else{
			qDebug("Frame Timed Out");
			continue;
		}

		
		imBuffer[(frameCount+1)%imBuffer.size()] = im;
		frameCount++;
		
		QueryPerformanceCounter(&pcToc);
		frameInterval = 1000.0*double(pcToc.QuadPart - pcTic.QuadPart)/double(pcFreq.QuadPart);
		QueryPerformanceCounter(&pcTic);

		if(expActive){
			//Call analysis function on the new image from within this thread
			QueryPerformanceCounter(&pcATic);
			gui->expAgent->analysisMod->larvaFind(
				imBuffer[frameCount%imBuffer.size()],
				width,height,frameCount);
			QueryPerformanceCounter(&pcAToc);

			//Track algorithm performance (in ms)
			gui->expAgent->analysisMod->analysisTime[frameCount%gui->expAgent->analysisMod->nBuffers] = 
				1000.0*double(pcAToc.QuadPart-pcATic.QuadPart)/double(pcFreq.QuadPart);
		}

		QueryPerformanceCounter(&pcToc);
		analysisInterval = 1000.0*double(pcToc.QuadPart - pcTic.QuadPart)/double(pcFreq.QuadPart);
		QueryPerformanceCounter(&pcTic);
				
		emit frameCaptured();
		
	}

	
	camera.AcquisitionStop.Execute();
	stream.CancelGrab();
	if(frameCount>-1) while(stream.GetWaitObject().Wait(0)) stream.RetrieveResult(Result[(frameCount)%nBuffers]);

	for (int j=0; j<nBuffers; ++j){
		stream.DeregisterBuffer(handles[j]);
		delete [] ppBuffers[j];
	}

	stream.FinishGrab();
	stream.Close();
	
	frameCount = -1;
	exited = true;
};

//Blocking functions to pause or unpause acquisition (can potentially lose frames, so should only be used at the end)
void CoreCamThread::pauseAcquisition(){
	if (stopped) return;
	pause = true;
	while(!paused) QThread::msleep(1);
};
void CoreCamThread::resumeAcquisition(){
	if (stopped) return;
	pause = false;
	while(paused) QThread::msleep(1);
};

void CoreCamThread::stop(){
	stopped = true;
	while(!exited){
		stopped = true;
		Sleep(1);
	}
};

void CoreCamThread::getRoiLimits(int *hMax, int *vMax){
	
	*hMax = camera.WidthMax.GetValue(); *vMax = camera.HeightMax.GetValue();
};
void CoreCamThread::updateRoi(){
	
	left = camera.OffsetX.GetValue();
	top = camera.OffsetY.GetValue();
	width = camera.Width.GetValue();
	height = camera.Height.GetValue();

};
void CoreCamThread::setRoi(unsigned short x, unsigned short y, unsigned short w, unsigned short h){

	//Cannot change size while camera is running
	if((w != width | h != height)){
		stop();
		camera.Width.SetValue(w);
		camera.Height.SetValue(h);
		start();
	}

	camera.OffsetX.SetValue(x);
	camera.OffsetY.SetValue(y);
	updateRoi();
};

void CoreCamThread::getBrightRange(int *min, int *max){
	*min = camera.BlackLevelRaw.GetMin(); *max = camera.BlackLevelRaw.GetMax();
};
void CoreCamThread::getGainRange(int *min, int *max){
	*min = camera.GainRaw.GetMin(); *max = camera.GainRaw.GetMax();
};
void CoreCamThread::getGammaRange(int *min, int *max){
	*min = (int)camera.Gamma.GetMin(); *max = (int)camera.Gamma.GetMax();
};
void CoreCamThread::getShutterRange(int *min, int *max){
	*min = camera.ExposureTimeRaw.GetMin(); *max = camera.ExposureTimeRaw.GetMax();
};
void CoreCamThread::updateProps(){
	brightness = camera.BlackLevelRaw.GetValue();
	gain = camera.GainRaw.GetValue();
	gamma = 0;//(int)camera.Gamma.GetValue();
	shutter = camera.ExposureTimeRaw.GetValue();
};

void CoreCamThread::setProps(unsigned short br, unsigned short gn, unsigned short gm, unsigned short sh){
	camera.BlackLevelRaw.SetValue((int64_t)br);
	camera.GainRaw.SetValue((int64_t)gn);
	camera.ExposureTimeRaw.SetValue((int64_t)sh);
	
	updateProps();
};