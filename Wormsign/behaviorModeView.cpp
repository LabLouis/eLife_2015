#include "behaviorModeView.h"
#include "stimThread.h"
#include "experimentAgent.h"
#include "analysisModule.h"

BehaviorModeView::BehaviorModeView(Wormsign * ui):QWidget((QWidget*)ui){

	this->setAutoFillBackground(true);
	this->setPalette(QPalette(QColor(200,180,180)));

	QLabel * tempLabel = new QLabel("Behavior Mode Viewer & Stimulus Indicator",this);
	tempLabel->setGeometry(10,5,300,20);
	QFont fnt = tempLabel->font(); fnt.setBold(true); tempLabel->setFont(fnt);

	gui = ui;

};
BehaviorModeView::~BehaviorModeView(){};

void BehaviorModeView::paintEvent(QPaintEvent *){

	QPainter painter(this);

	int width = geometry().width();
	int height = geometry().height();

	
	//Determine Behavior State
	AnalysisModule::BehaviorState state = AnalysisModule::NONE;
	bool stimulating = false;
	if(gui->expAgent!=0 & !gui->stimThread->stopped){
		if(gui->stimThread->index != -1){
			int ind = gui->stimThread->index % gui->expAgent->analysisMod->nBuffers;
			state = gui->expAgent->analysisMod->behaviorState[ind];

			//Determine if a stimulus is being delivered
			if(ind > 3){
				for(int i=0; i<4; i++)
					stimulating |= (gui->stimThread->stimAmps[ind-i] > 0);
			}else{
				stimulating = gui->stimThread->stimAmps[ind] > 0;
			}
		}
	}
	

	//Draw Rectangles w/ Colors
	QPolygonF rect;
	QPolygonF rectBase(4);
	rectBase[0] = QPointF(0,0);
	rectBase[1] = QPointF(90,0);
	rectBase[2] = QPointF(90,50);
	rectBase[3] = QPointF(0,50);

	QFont fnt = painter.font();
	fnt.setPointSize(14); fnt.setBold(true);
	painter.setFont(fnt);

	//Run
	if(state == AnalysisModule::RUN) painter.setBrush(QBrush(QColor(0,200,0)));
	else painter.setBrush(QBrush(QColor(200,0,0)));
	rect = rectBase;
	rect.translate(width/2-45,30);
	painter.drawPolygon(rect);
	painter.drawText(rect[0].x(),rect[0].y(),90,50,Qt::AlignCenter,"RUN");

	//Turn Left
	if(state == AnalysisModule::TLEFT) painter.setBrush(QBrush(QColor(0,200,0)));
	else painter.setBrush(QBrush(QColor(200,0,0)));
	rect = rectBase;
	rect.translate(width/4-65,70);
	painter.drawPolygon(rect);
	painter.drawText(rect[0].x(),rect[0].y(),90,50,Qt::AlignCenter,"Turn\nLeft");

	//Turn Right
	if(state == AnalysisModule::TRIGHT) painter.setBrush(QBrush(QColor(0,200,0)));
	else painter.setBrush(QBrush(QColor(200,0,0)));
	rect = rectBase;
	rect.translate(width*3/4-25,70);
	painter.drawPolygon(rect);
	painter.drawText(rect[0].x(),rect[0].y(),80,50,Qt::AlignCenter,"Turn\nRight");

	//Stop
	if(state == AnalysisModule::STOP) painter.setBrush(QBrush(QColor(0,200,0)));
	else painter.setBrush(QBrush(QColor(200,0,0)));
	rect = rectBase;
	rect.translate(width/2-45,110);
	painter.drawPolygon(rect);
	painter.drawText(rect[0].x(),rect[0].y(),90,50,Qt::AlignCenter,"Stop");

	//Cast Left
	if(state == AnalysisModule::CLEFT) painter.setBrush(QBrush(QColor(0,200,0)));
	else painter.setBrush(QBrush(QColor(200,0,0)));
	rect = rectBase;
	rect.translate(width/4-65,150);
	painter.drawPolygon(rect);
	painter.drawText(rect[0].x(),rect[0].y(),90,50,Qt::AlignCenter,"Cast\nLeft");

	//Cast Right
	if(state == AnalysisModule::CRIGHT) painter.setBrush(QBrush(QColor(0,200,0)));
	else painter.setBrush(QBrush(QColor(200,0,0)));
	rect = rectBase;
	rect.translate(width*3/4-25,150);
	painter.drawPolygon(rect);
	painter.drawText(rect[0].x(),rect[0].y(),90,50,Qt::AlignCenter,"Cast\nRight");

	//Back Up
	if(state == AnalysisModule::BACKUP) painter.setBrush(QBrush(QColor(0,200,0)));
	else painter.setBrush(QBrush(QColor(200,0,0)));
	rect = rectBase;
	rect.translate(width/2-45,190);
	painter.drawPolygon(rect);
	painter.drawText(rect[0].x(),rect[0].y(),90,50,Qt::AlignCenter,"Back Up");


	//STIMULUS
	if(stimulating) painter.setBrush(QBrush(QColor(100,100,255)));
	else painter.setBrush(QBrush(QColor(200,200,200)));
	rect[0] = QPointF(0,0);
	rect[1] = QPointF(200,0);
	rect[2] = QPointF(200,30);
	rect[3] = QPointF(0,30);
	rect.translate(width/2-100,250);
	painter.drawPolygon(rect);
	painter.drawText(rect[0].x(),rect[0].y(),200,30,Qt::AlignCenter,"Stimulating");
	

	

	
};