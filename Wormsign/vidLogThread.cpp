#include "vidLogThread.h"
#include "stimThread.h"


VidLogThread::VidLogThread(QString fileName,cv::Size imSize, bool annotate, AnalysisModule * aM, int nB, Wormsign * ui):QThread(){

	gui = ui;
	logAnnotate = annotate;
	aMod = aM;
	fName = fileName;

	exited = true; stopped = true; 

	nBuffers = nB;
	imBuffer.resize(nBuffers);

	width = imSize.width; height = imSize.height;

	thisImg = -1; latestImg = -1;
	frameCount = 0;
	framesLogged = 0;

	start();

	while(stopped) QThread::msleep(1);

	qDebug("filename: %fms",fName);
};
VidLogThread::~VidLogThread(){
	stop();
};
bool VidLogThread::addFrame(uchar * im){
	//check for buffer overrun
	//if((latestImg+1 - thisImg)>nBuffers) return false;

	int nextInd = (latestImg+1)%nBuffers;
	
	imBuffer[nextInd] = im;
	latestImg++;
	frameCount++;

	return true;
};
void VidLogThread::run(){
	
	thisImg = -1; latestImg = -1;
	frameCount = 0;
	framesLogged = 0;
	cv::Mat gImgH(cv::Size(width,height),CV_8UC1,NULL);
	cv::Mat cImg(cv::Size(width,height),CV_8UC3);

	std::string vidString = fName.toAscii().constData();
	cv::VideoWriter * vidLogger = new cv::VideoWriter;
	qDebug("vidString = %s,%d,%f,%d,%d,",vidString.c_str(),CV_FOURCC('X','V','I','D'),1000.0/double(aMod->frameIntervalMS),
		width,height);

//	vidLogger->open(vidString,CV_FOURCC('M','P','4','2'),1000.0/double(aMod->frameIntervalMS),
//		cv::Size(width,height),true);
	vidLogger->open(vidString,-1,1000.0/double(aMod->frameIntervalMS),
		cv::Size(width,height),true);

	exited = false;
	stopped = false;

	LARGE_INTEGER pcFreq,pcTic,pcToc;
	double pcDiff;
	QueryPerformanceFrequency(&pcFreq);

	int ind;

	while(!stopped){
		if(thisImg != latestImg){
			QueryPerformanceCounter(&pcTic);

			//wrap cv::Mat header on image buffer
			ind = (thisImg+1);
			gImgH.data = imBuffer[ind % nBuffers];
			
			//convert to color image (required for compression)
			cv::cvtColor(gImgH,cImg,CV_GRAY2BGR);

			//annotate image
			if(logAnnotate) annotateImage(cImg,ind);

			//Write frame
			(*vidLogger) << cImg;
			
			thisImg++;
			framesLogged++;
			QueryPerformanceCounter(&pcToc);
			pcDiff = double(pcToc.QuadPart-pcTic.QuadPart)/double(pcFreq.QuadPart);
			if(logAnnotate) qDebug("Write Profile: %fms",pcDiff);
		}else{
			//wait for more frames
			QThread::msleep(1);
		}
	}

	delete vidLogger;
	exited = true;
};
void VidLogThread::stop(){

	while((thisImg != latestImg) & !stopped) QThread::msleep(1);

	stopped = true;
	while(!exited){
		stopped = true;
		QThread::msleep(1);
	}

};

/*Takes the output RGB image and draws the analysis results as they appear in the camera
   visualization window.  Used for presentations and demos.  Could be stripped out and put into
   a stand-alone program to convert 

    1) Am not logging raw animal contour
	2) Log skeleton in history along with other data in aMod - update all references to the skeleton variable accordingly

	Also, add field image to overlay
	
*/
void VidLogThread::annotateImage(cv::Mat & img,int ind){
	
	ind = ind % aMod->nBuffers;
	double xval, yval;

	//Visualize stimulus by modulating global blue level
	uchar offset = gui->stimThread->stimAmps[ind]*40/100;
	if(offset!=0)
		for(int i=0; i<img.rows*img.cols*3; i+=3) img.data[i] += offset;

	//Draw Raw Contour
	cv::drawContours(img,aMod->cLarva,ind,cv::Scalar(0,180,180));
	
	//Draw Reconstructed Fourier Contour
	std::vector<std::vector<cv::Point>> cFitD; cFitD.resize(1); cFitD[0].resize(200);
	std::vector<cv::Point2f> cFit;
	aMod->fourierReconstruct(aMod->fourier[ind],cFit,200);
	for(int i=0; i<200; i++) {cFitD[0][i].x = cFit[i].x; cFitD[0][i].y = cFit[i].y;}
	cv::drawContours(img,cFitD,0,cv::Scalar(0,180,0));
	
	//Draw Skeleton
	for(int i=1; i<aMod->skeleton.size(); i++){
		cv::line(img,aMod->skeleton[i],aMod->skeleton[i-1],cv::Scalar(0,0,255),2);
	}

	
	
	//Draw Tail-Neck Line - projecting past neck
	xval = aMod->neck[ind].x - 100.0*cos(aMod->tailBearingAngle[ind]);
	yval = aMod->neck[ind].y - 100.0*sin(aMod->tailBearingAngle[ind]);
	cv::line(img,cv::Point(xval,yval),aMod->tail[ind],cv::Scalar(0,255,255),2);
	
	//Draw Neck-Head Line
	cv::line(img,aMod->head[ind],aMod->neck[ind],cv::Scalar(0,255,255),2);
	//Draw tail back fit bearing line
	
	//Draw Neck point
	cv::circle(img,aMod->neck[ind],2,cv::Scalar(255,200,255));
	//Draw Centroid
	cv::circle(img,aMod->centroid[ind],2,cv::Scalar(255,255,255));
	//Draw Head and Tail Points
	cv::circle(img,aMod->head[ind],2,cv::Scalar(0,0,255));
	cv::circle(img,aMod->tail[ind],2,cv::Scalar(255,0,0));
	//Draw Text at head labeling it as such
	cv::putText(img,"Head",aMod->head[ind],cv::FONT_HERSHEY_COMPLEX_SMALL,1,cv::Scalar(0,0,255));
	
	
	//Annotate with performance and Date/Time info in the upper left
	cv::putText(img,"HHMI Janelia Farm Research Campus",cv::Point(10,20),cv::FONT_HERSHEY_COMPLEX_SMALL,1,cv::Scalar(0,255,255));
	cv::putText(img,QDateTime::currentDateTime().toString(Qt::SystemLocaleShortDate).toAscii().constData(),cv::Point(10,40),cv::FONT_HERSHEY_COMPLEX_SMALL,1,cv::Scalar(0,255,255));

	//Draw Biorule name
	QString ruleName = "Rule: " + gui->bioRuleList->currentText();
	cv::putText(img,ruleName.toAscii().constData(),cv::Point(10,60),cv::FONT_HERSHEY_COMPLEX_SMALL,1,cv::Scalar(0,255,255));

	//Behavior State
	QString bState = "State: ";
	switch(aMod->behaviorState[ind]){
		case AnalysisModule::RUN:		bState += "Run";		break;
		case AnalysisModule::TLEFT:		bState += "Turn Left";	break;
		case AnalysisModule::TRIGHT:	bState += "Turn Right";	break;
		case AnalysisModule::STOP:		bState += "Stop";		break;
		case AnalysisModule::CLEFT:		bState += "Cast Left"; 	break;
		case AnalysisModule::CRIGHT:	bState += "Cast Right";	break;
		case AnalysisModule::BACKUP:	bState += "Back Up";	break;
	}
	cv::putText(img,bState.toAscii().constData(),cv::Point(10,120),cv::FONT_HERSHEY_COMPLEX_SMALL,1,cv::Scalar(0,0,255));

	
	//Draw Arena Inset
	cv::Mat fieldImage = gui->expAgent->makeFieldImage();
	int colOffset = img.cols - fieldImage.cols - 1;
	for(int i=0; i<fieldImage.rows; i++)
		for(int j=0; j<fieldImage.cols; j++){
			img.data[ 3*(i*img.cols + colOffset+j) ] = fieldImage.data[3*(i*fieldImage.cols + j)];
			img.data[ 3*(i*img.cols + colOffset+j) + 1 ] = fieldImage.data[3*(i*fieldImage.cols + j) + 1];
			img.data[ 3*(i*img.cols + colOffset+j) + 2 ] = fieldImage.data[3*(i*fieldImage.cols + j) + 2];

		}

};
