#include "experimentAgent.h"
#include "stageThread.h"
#include "stimThread.h"
#include "arenaTrack.h"
#include "vidLogThread.h"

//Initialize experiment
ExperimentAgent::ExperimentAgent(Wormsign * ui,bool log):QThread(){
	stopped = true; exited = true;

	gui = ui;
	hrs = 0; mins = 0; secs = 0;

	//Create Analysis Module for storing data
	analysisMod = new AnalysisModule(gui);
	analysisMod->frameIntervalMS = gui->frameIntIn->value();
	
	gui->markupLog->setEnabled(false);
	gui->vidLog->setEnabled(false);
	gui->dataLog->setEnabled(false);

	start();
};

ExperimentAgent::~ExperimentAgent(){

};


//Logging & monitoring thread
void ExperimentAgent::run(){

	//initiate experiment
	initExperiment();	

	exited = false;
	stopped = false;
	errorEmitted = false;
	index = -1;
	int ind;

	while(!stopped){
		if(gui->stimThread->index != index){
			//monitor for new frames, log new frames when analysis is complete and feedback has been achieved
			updateTime();
			
			//log data
				//video
				ind = (index+1);

				if(logVid)
					vidLogger->addFrame(gui->camThread->imBuffer[ind%gui->camThread->nBuffers]);

				if(logAnnotate)
					markupLogger->addFrame(gui->camThread->imBuffer[ind%gui->camThread->nBuffers]);
				
				//analysis
				if(logData){
					ind = (index+1) % analysisMod->nBuffers;
					dataStream << analysisMod->makeDataLine(ind);
					dataStream.flush();
				}
			
			//monitor for new data, send to ui and trigger update
			index++;
		}else{
			QThread::msleep(1);

			//If biorules error detected, quit early
			if(gui->stimThread->errorExit & !errorEmitted){
				//Quit early, popup a window
				emit errorDetected();
				errorEmitted = true;
			}
		}	

	}

	//close experiment
	closeExperiment();

	exited = true;

};

//Blocking function ends the log monitoring thread
void ExperimentAgent::stop(){
	stopped = true;
	while(!exited){
		stopped = true;
		Sleep(1);
	}

	gui->markupLog->setEnabled(true);
	gui->vidLog->setEnabled(true);
	gui->dataLog->setEnabled(true);
};

void ExperimentAgent::updateTime(){

	int tsecs = (analysisMod->frameIntervalMS*gui->camThread->frameCount)/1000;
	secs = tsecs % 60;
	mins = ((tsecs-secs)/60) % 60;
	hrs = ((tsecs - secs - mins*60)/(60*60));
	
	totalsecs = tsecs;

};

void ExperimentAgent::initExperiment(){
	
	gui->stageThread->stop(); //Stop stage 
	gui->camThread->stop();   //Stop cameras

	logAnnotate = gui->markupLog->isChecked();
	logVid = gui->vidLog->isChecked();
	logData = gui->dataLog->isChecked();

	QDir pathDir;

	//If logging any data
	if(logAnnotate | logVid | logData){
		//Create experiment directory in the target dir, name based on date-time
		dateTime = QDateTime::currentDateTime();
		froot = dateTime.toString("yyyyMMdd-hhmmss");
		pathDir = QDir(gui->expDirectory->text());
		pathDir.mkdir(froot);
		pathDir.cd(froot);
	}

	

	if(logData){
	//Store Experiment Config Data - includes data legend
		configFile.setFileName(pathDir.toNativeSeparators(pathDir.path()) + "\\" + froot + "_configuration.txt");
		configFile.open(QIODevice::WriteOnly | QIODevice::Text);
		otherStream.setDevice(&configFile);
		otherStream << createConfigString();
		otherStream.flush();
		configFile.close();
	}
		
	//Create Log targets for
	//Video
	QString vidName;
	std::string vidString;

	if(logVid){

		vidName = pathDir.toNativeSeparators(pathDir.path()) + "\\" + froot + "_video.avi";
		vidLogger = new VidLogThread(vidName,cv::Size(gui->camThread->width,gui->camThread->height),
			false,analysisMod,gui->camThread->nBuffers,gui);

	}

	
	if(logAnnotate){
		//Annotated Video
		vidName = pathDir.toNativeSeparators(pathDir.path()) + "\\" + froot + "_annotatedVideo.avi";
		markupLogger = new VidLogThread(vidName,cv::Size(gui->camThread->width,gui->camThread->height),
			true,analysisMod,gui->camThread->nBuffers,gui);
	}
		
	if(logData){
	//Analysis results and stimulation data
		dataFile.setFileName(pathDir.toNativeSeparators(pathDir.path()) + "\\" + froot + "_data.txt");
		dataFile.open(QIODevice::WriteOnly | QIODevice::Text);
		dataStream.setDevice(&dataFile);
	}


	//Start Stimulus thread, will monitor camthread for new frames and start triggering
	//Does not automatically start triggering
	gui->stimThread->start();

	//Start Stage thread - will monitor for new analysis results to send to stage, requests position otherwise
	gui->stageThread->state = StageThread::EXPERIMENT;
	gui->stageThread->start();
		
	//configure cameras for HW trigger mode
	gui->camThread->setTrig(true);
	//start camera thread in experiment mode
	gui->camThread->start();
	
	while(gui->camThread->stopped) Sleep(1); //wait for cameras to come up
	while(gui->stimThread->stopped) Sleep(1);
	while(gui->stageThread->stopped) Sleep(1);
	
	//All systems are up and running, start triggering camera frames
	gui->stimThread->sendStartSignalCOM(analysisMod->frameIntervalMS); 
	
		

};
void ExperimentAgent::closeExperiment(){

	//coordinate stopping by pausing camera thread, then wait for stim to catch up
	gui->camThread->pauseAcquisition();

	//wait for cameras to finish up, log final frames and data to vidlogger and datafile
	int ind;
	while(index < gui->stimThread->index){
		if(gui->stimThread->index != index){
			
			//Log remaining video frames
			ind = (index+1);
			if(logVid)	vidLogger->addFrame(gui->camThread->imBuffer[ind%gui->camThread->nBuffers]);
			
			if(logAnnotate) markupLogger->addFrame(gui->camThread->imBuffer[ind%gui->camThread->nBuffers]);
				
			//log remaining analysis results w/ stimulation data
			if(logData){
				ind = (index+1) % analysisMod->nBuffers;
				dataStream << analysisMod->makeDataLine(ind);
				dataStream.flush();
			}
			index++;
		}else{
			QThread::msleep(1);
		}
	}
	qDebug("Logged %d Frames",index+1);
	//Close files
	if(logVid) {vidLogger->stop(); delete vidLogger;}
	if(logAnnotate)	{markupLogger->stop(); delete markupLogger;}
	if(logData){
		dataStream.flush();
		dataFile.close();
	}

	QDir pathDir;
	pathDir = QDir(gui->expDirectory->text());
	pathDir.cd(froot);

	if(logAnnotate | logVid | logData){
		//write field image	
		
		QString fieldFname = pathDir.toNativeSeparators(pathDir.path()) + "\\" + froot + "_fieldImage.png";
		cv::Mat fieldImage = makeFieldImage();		
		cv::imwrite(fieldFname.toAscii().constData(),fieldImage);
	}

	


	//reset interfaces
	
	//This will log the experimental parameters to disk - 
	//		rules server only callable from the thread it was created in
	if(logAnnotate | logVid | logData) gui->stimThread->logParameters = true;
	gui->stimThread->stop();  //won't return until stimthread reads response from eric's server
	//at this point, stimThread->paramResponse should contain xml output from Eric's rules app
	// Write it to a file
	if(logAnnotate | logVid | logData){
		configFile.setFileName(pathDir.toNativeSeparators(pathDir.path()) + "\\" + froot + "_bioruleParameters.txt");
		configFile.open(QIODevice::WriteOnly | QIODevice::Text);
		otherStream.setDevice(&configFile);
		otherStream << gui->biorulesConnection->paramResponse;
		otherStream.flush();
		configFile.close();
	}


	gui->stageThread->stop();
	gui->camThread->stop();
	
	gui->camThread->setTrig(false);
	gui->camThread->start();

	gui->stageThread->state = StageThread::POLLPOSOTION;
	gui->stageThread->start();

	delete analysisMod;
};

//XML file with current experiment configuration
QString ExperimentAgent::createConfigString(){

	QString config;
	//Write Date/Time of experiment start

	config += "Wormsign Larva Tracking Experiment Configuration File: \n";
	config += dateTime.toString() + "\n";
	config += dateTime.toString("yyyyMMdd-hhmmss") + "\n\n";
	
	config += "WormSign Version: " + gui->appVersion + "\n\n";

	config += "BioRule Used: " + gui->bioRuleList->currentText() + "\n";
	config += "BioRule Version: " + gui->biorulesConnection->bioRulesVersion + "\n";
	config += "BioRule Message Version: " + gui->biorulesConnection->messageVersion + "\n\n";

	config += "Stimulus/Trigger Box Version: " + gui->stimThread->stimVersion + "\n\n";
	
	config += QString().sprintf("Camera Calibration (um per pixel): %.1f\n",gui->camThread->umPerPixel);
	config += QString().sprintf("Stage Calibration (tick per mm): x = %.0f, y= %.0f\n\n",
		gui->stageThread->tickPerMM_X,gui->stageThread->tickPerMM_Y);
	
	config += QString().sprintf("Camera Frame Trigger Interval (ms): %d\n\n",analysisMod->frameIntervalMS);

	//Camera properties
	config += "Camera: " + gui->camList->currentText() + "\n";
	//roi
	config += QString().sprintf("Camera ROI: %d, %d, %d, %d\n",
		gui->camThread->left, gui->camThread->top, gui->camThread->width, gui->camThread->height);
	
	//brightness
	config += QString().sprintf("Camera Brightness: %d\n",gui->camThread->brightness);
	//gain
	config += QString().sprintf("Camera Gain: %d\n",gui->camThread->gain);
	//gamma
	config += QString().sprintf("Camera Gamma: %d\n",gui->camThread->gamma);
	//shutter
	config += QString().sprintf("Camera Shutter: %d\n",gui->camThread->shutter);

	config += "\n";

	//vidlogfile name (if any)
	config += "Video File Name: " + froot + "_video.avi\n";
	//results log file name (if any)
	config += "Results File: " + froot + "_data.txt\n";

	config += "\nNeck Location (% from head): " + QString().sprintf("%.2f\n",100*analysisMod->neckPercentage);

	config += "\nBehavior State Codes:\n";
	config += QString().sprintf("%d: None or Unrecognized Response\n",AnalysisModule::NONE);
	config += QString().sprintf("%d: Run\n",AnalysisModule::RUN);
	config += QString().sprintf("%d: Turn Left\n",AnalysisModule::TLEFT);
	config += QString().sprintf("%d: Turn Right\n",AnalysisModule::TRIGHT);
	config += QString().sprintf("%d: Stop\n",AnalysisModule::STOP);
	config += QString().sprintf("%d: Cast Left\n",AnalysisModule::CLEFT);
	config += QString().sprintf("%d: Cast Right\n",AnalysisModule::CRIGHT);
	config += QString().sprintf("%d: Back Up\n",AnalysisModule::BACKUP);

	config += "\nData File Legend:\n";
	config += analysisMod->makeDataLegend();

	return config;


};


//Write the light landscape with the animal's track to an image file
cv::Mat ExperimentAgent::makeFieldImage(){

	AnalysisModule * aMod = gui->expAgent->analysisMod;
	double xval,yval, xvalp, yvalp;
	uchar val;
	int imgIndex = gui->bioRuleList->currentIndex();
	cv::Mat fieldImage;
	//from arenatrack paintEvent
	double boxSize = 240; //in pixels on screen
	double realSize = 400; // in mm
	double tickPerMMX = gui->stageThread->tickPerMM_X;
	double tickPerMMY = gui->stageThread->tickPerMM_Y;

	bool landscapePresent;

	if(gui->biorulesConnection->fieldImages[imgIndex].empty()){
		fieldImage.create(240,240,CV_8UC3);
		fieldImage.setTo(cv::Scalar(0));
		landscapePresent = false;

	}else{
		fieldImage = gui->biorulesConnection->fieldImages[imgIndex].clone();
		landscapePresent = true;
		//flip RGB to BGR
		for(int i=0; i<fieldImage.rows*fieldImage.cols; i++){
			if(fieldImage.data[i*3+2] > 0){
				fieldImage.data[i*3] = fieldImage.data[i*3+2];
				fieldImage.data[i*3+2] = 0;
			}
		}
	}

	//draw track
	cv::Scalar color(255,255,255);
	for(int i=1; i<aMod->fullTrackInd; i++){
		xvalp = aMod->fullTrack[i-1].x*(boxSize/realSize);
		yvalp = aMod->fullTrack[i-1].y*(boxSize/realSize);
		xval = aMod->fullTrack[i].x*(boxSize/realSize);
		yval = aMod->fullTrack[i].y*(boxSize/realSize);
		if(!landscapePresent){
			val = uchar(255.0*aMod->fullTrackStim[i]/100.0);
			color = cv::Scalar(255,255-val,255-val);
		}
		cv::line(fieldImage,cv::Point(int(xvalp),int(yvalp)),cv::Point(int(xval),int(yval)),color);
	}

	cv::circle(fieldImage,cv::Point(xval,yval),1,cv::Scalar(0,0,255));
	
	return fieldImage;
};