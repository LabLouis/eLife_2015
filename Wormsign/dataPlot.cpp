#include "dataPlot.h"
#include "experimentAgent.h"
#include "analysisModule.h"
#include "stimThread.h"
#include "stageThread.h"
#include "coreCamThread.h"

DataPlot::DataPlot(Wormsign * ui):QWidget((QWidget*)ui){

	this->setAutoFillBackground(true);
	this->setPalette(QPalette(QColor(200,180,180)));

	gui = ui;

	historyDepth = 600;
	plotData.resize(historyDepth);
	stimData.resize(historyDepth);

	parameterList = new QComboBox(this);
	parameterList->setGeometry(10,10,200,20);
	QStringList parameters;
	parameters << "Head to Body Angle (deg)";
	parameters << "Body Bearing (deg)";
	parameters << "Head Speed (mm/s)";
	parameters << "Tail Speed (mm/s)";
	parameters << "Body Bearing Derivative (deg/s)";
	parameterList->insertItems(0,parameters);



};
DataPlot::~DataPlot(){};

void DataPlot::paintEvent(QPaintEvent *){
	
	QPainter painter(this);
	double xval, yval, tickVal, tickStep;
	int nTicks;
	
	//Draw Graph frame
	double width = geometry().width();
	double height = geometry().height();
	double left = width*.1;
	double right = width*.9;
	double top = height*.2;
	double bottom = height*.85;

	double yOffset, yTickStep;
	switch(parameterList->currentIndex()){
		case 0: // "Head to Body Angle (deg)"
			yOffset = -180;
			yTickStep = 90;
			break;
		case 1: // "Body Bearing (deg)"
			yOffset = -180;
			yTickStep = 90;
			break;
		case 2: // "Head Velocity (mm/s)"
			yOffset = 0;
			yTickStep = 2;
			break;
		case 3: // "Tail Velocity (mm/s)"
			yOffset = 0;
			yTickStep = 2;
			break;
		case 4: // "Body Bearing Derivative (deg/s)
			yOffset = -30;
			yTickStep = 15;
			break;
	}

	//left vertical bar
	painter.drawLine(left,top,left,bottom);
	//right bar = axis w/ labels (ms)
	painter.drawLine(right,top,right,bottom);
	yval = bottom;
	xval = right;
	tickVal = 0;
	nTicks = 5;
	while(yval > top){
		painter.setPen(QColor(0,0,0));
		painter.drawLine(xval,yval,xval-10,yval);
		painter.drawText(xval+5,yval+5,QString().sprintf("%.0f",tickVal+yOffset));
		
		//draw dashed grid lines
		if(yval != bottom){
			painter.setPen(QPen(QBrush(QColor(100,100,100)),1,Qt::DashLine));
			painter.drawLine(left,yval,right,yval);
		}

		yval -= (bottom-top)/double(nTicks);
		tickVal += yTickStep; //10ms steps
	}
	double yscale = (bottom - yval)/tickVal;
	
	painter.setPen(QPen(QBrush(QColor(0,0,0)),1,Qt::SolidLine));

	//bottom bar = axis w/ labels (time)
	painter.drawLine(left,bottom,right,bottom);
	yval = bottom;
	xval = right;
	tickVal = 0;
	nTicks = 5;
	tickStep = floor( .5 + (double(historyDepth*gui->frameIntIn->value())/1000.0)/double(nTicks));  //round
	double xStep = (right-left)*tickStep/(double(historyDepth*gui->frameIntIn->value())/1000.0);
		
	while(xval>left){
		painter.drawLine(xval,yval,xval,yval-10);
		painter.drawText(xval-2,yval+12,QString().sprintf("%.0f",tickVal));
		xval -= xStep;  //calculate steps
		tickVal += tickStep;
	}
	
	painter.drawText(width*.4,bottom + height*.1,QString("History (sec)"));

	//painter.rotate(-90);
	//painter.drawText(-height*.7,width*.99,QString("Closed Loop Time (ms)"));
	//painter.rotate(90);

	//if not data available, return
	if(!gui->expAgent) return;
	if(!gui->camThread) return;

	double tickPerMMX = gui->stageThread->tickPerMM_X;
	double tickPerMMY = gui->stageThread->tickPerMM_Y;
	double umPerPixel = gui->camThread->umPerPixel;


	//build data
	if(plotData.size() != historyDepth) plotData.resize(historyDepth);
	if(stimData.size() != historyDepth) stimData.resize(historyDepth);
	xStep = (right-left)/plotData.size();
	xval = left;
	int indRoot = gui->stimThread->index - historyDepth;
	int ind;
	double accumulator;
	double normFactor = 0;
	double filterHistory = 60;
	for(int i=1; i<=filterHistory; i++) normFactor+=i;

	int headAngleHistoryStep = 1;
	double dt = double(headAngleHistoryStep*gui->expAgent->analysisMod->frameIntervalMS)/1000.0; //time interval into history in seconds

	for(int i=0; i<historyDepth; i++){
		ind = indRoot + i;
		if(ind < 0){
			yval = bottom;
		}else{
			switch(parameterList->currentIndex()){
				case 0: // "Head to Body Angle (deg)"
					yval = gui->expAgent->analysisMod->headToBodyAngle[ind % gui->expAgent->analysisMod->nBuffers]*180/M_PI - yOffset;
					yval = bottom - yval*yscale;
					break;
				case 1: // "Body Bearing (deg)"
					//yval = gui->expAgent->analysisMod->tailBearingAngle[ind % gui->expAgent->analysisMod->nBuffers]*180/M_PI - yOffset;
					yval = gui->expAgent->analysisMod->newTailBearing[ind % gui->expAgent->analysisMod->nBuffers]*180/M_PI - yOffset;
					yval = bottom - yval*yscale;
					break;
				case 2: // "Head Speed (mm/s)"
					yval = gui->expAgent->analysisMod->headSpeed[ind % gui->expAgent->analysisMod->nBuffers] - yOffset;
					yval = bottom - yval*yscale;
					break;
				case 3: // "Tail Speed (mm/s)"
					yval = gui->expAgent->analysisMod->tailSpeed[ind % gui->expAgent->analysisMod->nBuffers] - yOffset;
					yval = bottom - yval*yscale;
					break;
				case 4: // "Body Bearing Derivative (deg/s)

					yval = gui->expAgent->analysisMod->dNewTailBearing[ind % gui->expAgent->analysisMod->nBuffers]*180/M_PI;
					yval = bottom - (yval-yOffset)*yscale;
					break;
			}
			
		}
		//scale raw value to graphics
		plotData[i].setX(xval);
		plotData[i].setY(yval);
		
		xval += xStep;
	}

	//Plot stimulus in blue
	xval = left;
	yscale = (bottom-top)/100;
	for(int i=0; i<historyDepth; i++){
		ind = indRoot+i;
		if(ind < 0){
			yval = bottom;
		}else{
			ind = ind%gui->stimThread->stimAmps.size();
			yval = gui->stimThread->stimAmps[ind];
			yval = bottom - yval*yscale;
		}
		stimData[i].setX(xval);
		stimData[i].setY(yval);
		xval += xStep;
	}

	//plot stimulus in back in blue
	painter.setPen(QColor(0,0,200));
	painter.drawPolyline(stimData);

	//Plot Data in dark green
	painter.setPen(QColor(0,150,0));
	painter.drawPolyline(plotData);

	
};