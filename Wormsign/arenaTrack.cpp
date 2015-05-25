#include "arenaTrack.h"
#include "experimentAgent.h"
#include "analysisModule.h"
#include "stimThread.h"
#include "stageThread.h"
#include "coreCamThread.h"
#include "biorulesConnect.h"

ArenaTrack::ArenaTrack(Wormsign * ui):QWidget((QWidget*)ui){

	this->setAutoFillBackground(true);
	this->setPalette(QPalette(QColor(200,180,180)));

	QLabel * tempLabel = new QLabel("Animal Position History",this);
	tempLabel->setGeometry(10,5,200,20);
	QFont fnt = tempLabel->font(); fnt.setBold(true); tempLabel->setFont(fnt);

	gui = ui;
	plotData.resize(0);

};

ArenaTrack::~ArenaTrack(){};

void ArenaTrack::paintEvent(QPaintEvent*){
	
	QPainter painter(this);

	cv::Point2f offset(30,30);
	double boxSize = 240; //in pixels on screen
	double realSize = 400; // in mm

	//Determine if there is an image for a landscape display
	if(gui->bioRuleList->count() > 0){
		bool imageDisplay = false;
		int imgIndex = gui->bioRuleList->currentIndex();
		QImage img;
		cv::Mat * cvImg = &(gui->biorulesConnection->fieldImages[imgIndex]);
		if(gui->biorulesConnection->fieldImages[imgIndex].rows > 0){
			img = QImage(cvImg->data,cvImg->cols,cvImg->rows,cvImg->cols * 3,QImage::Format_RGB888);
			imageDisplay = true;
			painter.drawImage(QRectF(offset.x,offset.y,boxSize,boxSize),img);
		}
	}


	//Draw square (whole space is 310w by 300h)
	painter.drawRect(offset.x,offset.y,boxSize,boxSize);

	//draw animal path in the square
	
	//if not data available, return
	if(!gui->expAgent) return;
	if(gui->expAgent->analysisMod->index < 0) return;
	if(gui->stimThread->index < 0) return;
	if(gui->expAgent->analysisMod->fullTrackInd <0) return;



	//Calibration values
	double tickPerMMX = gui->stageThread->tickPerMM_X;
	double tickPerMMY = gui->stageThread->tickPerMM_Y;

	//build plotData (if a new sample is available)
	AnalysisModule * aMod = gui->expAgent->analysisMod;
	if(plotData.size() != (aMod->fullTrackInd+1)){
		plotData.resize(aMod->fullTrackInd+1);
		for(int i=0; i<=aMod->fullTrackInd; i++){
			plotData[i].setX(aMod->fullTrack[i].x*(boxSize/realSize) + offset.x);
			plotData[i].setY(aMod->fullTrack[i].y*(boxSize/realSize) + offset.y);
		}
	}

	
	
	//Plot Data in white (on black/blue background)
	painter.setPen(QColor(255,255,255));
	painter.drawPolyline(plotData);

	//Draw red dot at Animal
	painter.setPen(QPen(QBrush(QColor(255,0,0)),3));
	painter.drawPoint(plotData[plotData.size()-1]);
	
	
};