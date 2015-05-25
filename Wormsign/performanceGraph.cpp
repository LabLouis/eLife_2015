#include "performanceGraph.h"
#include "experimentAgent.h"
#include "analysisModule.h"
#include "stimThread.h"


PerformanceGraph::PerformanceGraph(Wormsign * ui):QWidget((QWidget*)ui){

	this->setAutoFillBackground(true);
	this->setPalette(QPalette(QColor(200,180,180)));

	historyDepth = 300; //initial history depth
	plotData.resize(historyDepth);
	algorithmData.resize(historyDepth);
	networkData.resize(historyDepth);
	exposureData.resize(historyDepth);

	gui = ui;

	bool newSize = true;



};
PerformanceGraph::~PerformanceGraph(){};

void PerformanceGraph::paintEvent(QPaintEvent *){
	
	QPainter painter(this);
	double xval, yval, aYval, nYval, eYval, tickVal, tickStep;
	int nTicks;
	double avgVal = 0, agvAVal = 0, avgNVal = 0, avgCnt = 0;
	double dataXferTime = 0;

	painter.drawText(5,20,"System Performance");
	
	//Draw Graph frame
	double width = geometry().width();
	double height = geometry().height();
	double left = width*.1;
	double right = width*.9;
	double top = height*.2;
	double bottom = height*.8;

	//left vertical bar
	painter.drawLine(left,top,left,bottom);
	//right bar = axis w/ labels (ms)
	painter.drawLine(right,top,right,bottom);
	yval = bottom;
	xval = right;
	tickVal = 0;
	nTicks = 6;
	while(yval > top){
		painter.setPen(QColor(0,0,0));
		painter.drawLine(xval,yval,xval-10,yval);
		painter.drawText(xval+5,yval+5,QString().sprintf("%.0f",tickVal));
		
		//draw dashed grid lines
		if(yval != bottom){
			painter.setPen(QPen(QBrush(QColor(100,100,100)),1,Qt::DashLine));
			painter.drawLine(left,yval,right,yval);
		}

		yval -= (bottom-top)/double(nTicks);
		tickVal += 10; //10ms steps
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

	painter.rotate(-90);
	painter.drawText(-height*.7,width*.99,QString("Closed Loop Time (ms)"));
	painter.rotate(90);

	//if not data available, return
	if(!gui->expAgent) return;

	
	//build data
	if(plotData.size() != historyDepth) {plotData.resize(historyDepth); newSize = true;}
	if(algorithmData.size() != historyDepth) {algorithmData.resize(historyDepth); newSize = true;}
	if(networkData.size() != historyDepth) {networkData.resize(historyDepth); newSize = true;}
	if(exposureData.size() != historyDepth) {exposureData.resize(historyDepth); newSize = true;}

	xStep = (right-left)/plotData.size();
	int indRoot = gui->stimThread->index - historyDepth;
	int ind;

	//only should need to update shutter once (or when we're initially filling data)
	eYval = double(gui->camThread->shutter) * .02; 
	xval = left;
	if(eYval != exposureData[0].y() | indRoot < 0){
		for(int i=0; i<historyDepth; i++){
			ind = indRoot + i;
			exposureData[i].setX(xval);
			if(ind < 0) exposureData[i].setY(bottom);
			else exposureData[i].setY(bottom-eYval*yscale);
			xval += xStep;
		}
	}

	xval = left;
	for(int i=0; i<historyDepth; i++){
		ind = indRoot + i;
		if(ind < 0){
			yval = bottom;
			aYval = bottom;
			nYval = bottom;
			
		}else{
			yval = gui->expAgent->analysisMod->loopTime[ind % gui->expAgent->analysisMod->nBuffers];
			aYval = gui->expAgent->analysisMod->analysisTime[ind % gui->expAgent->analysisMod->nBuffers];
			nYval = gui->expAgent->analysisMod->networkTime[ind % gui->expAgent->analysisMod->nBuffers];
			avgVal += yval; 
			agvAVal += aYval;
			avgNVal += nYval;
			avgCnt++;
			nYval = yval-nYval;
			aYval = nYval-aYval;
			dataXferTime += aYval - eYval;
			
	
			//scale raw value to graphics
			yval = bottom - yval*yscale;
			nYval = bottom - nYval*yscale;
			aYval = bottom - aYval*yscale;
		}
		
		if(newSize){  //prevents redundant filling of the x-axis values
			plotData[i].setX(xval);
			algorithmData[i].setX(xval);
			networkData[i].setX(xval);
		}

		plotData[i].setY(yval);
		algorithmData[i].setY(aYval);
		networkData[i].setY(nYval);
		
		xval += xStep;
	}
	if(newSize) newSize = false;

	//Plot Data in blue (total loop time)
	painter.setPen(QColor(0,0,255));
	painter.drawPolyline(plotData);
	painter.drawText(width*.35,height*.2-30,QString().sprintf("Total Loop Time: %.1f ms",avgVal/avgCnt));

	//Plot network performance time
	painter.setPen(QColor(0,150,0));
	painter.drawPolyline(networkData);
	painter.drawText(width*.35,height*.2-15,QString().sprintf("Network Time: %.1f ms",avgNVal/avgCnt));

	//Plot algorithm performance time
	painter.setPen(QColor(180,0,0));
	painter.drawPolyline(algorithmData);
	painter.drawText(width*.35,height*.2,QString().sprintf("Algorithm Time: %.1f ms",agvAVal/avgCnt));

	//Avg data transfer time
	painter.setPen(QColor(0,0,0));
	painter.drawText(width*.35,height*.2+15,QString().sprintf("Data Streaming Time: %.1f ms",dataXferTime/avgCnt));

	//Plot exposure Time (can probably trim this down to 3 points)
	painter.setPen(QColor(180,0,180));
	painter.drawPolyline(exposureData);
	painter.drawText(width*.35,height*.2+30,QString().sprintf("Exposure Time: %.1f ms",eYval));

};