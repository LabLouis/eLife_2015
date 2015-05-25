#include "manualControls.h"
#include "dataPlot.h"
#include "performanceGraph.h"
#include "experimentAgent.h"
#include "analysisModule.h"
#include "stimThread.h"

ManualControls::ManualControls(Wormsign * ui):QWidget((QWidget*)ui){

	this->setAutoFillBackground(true);
	this->setPalette(QPalette(QColor(200,180,180)));
	
	//manualControls->setGeometry(320,680,310,250);
	gui = ui;

	QLabel * tempLabel = new QLabel("Manual Experiment Controls",this);
	tempLabel->setGeometry(10,5,200,20);
	QFont fnt = tempLabel->font(); fnt.setBold(true); tempLabel->setFont(fnt);

	//layout manual controls
	crosshairCheck = new QCheckBox("Display Crosshairs (with 1mm ticks) in Visualization",this);
	crosshairCheck->setChecked(true);
	crosshairCheck->setGeometry(10,40,300,20);

	connect(crosshairCheck,SIGNAL(clicked()),this,SLOT(crosshairChange()));

	//Data history
	dataHistoryLength = new QSpinBox(this);
	dataHistoryLength->setGeometry(10,65,60,20);
	dataHistoryLength->setMinimum(3);
	dataHistoryLength->setMaximum(30);
	dataHistoryLength->setValue(10);
	tempLabel = new QLabel("Data Visualization History (sec)",this);
	tempLabel->setGeometry(80,65,200,20);
	
	connect(dataHistoryLength,SIGNAL(valueChanged(int)),this,SLOT(historyChange(int)));
	
	clearVotes = new QPushButton("Clear H/T Votes",this);
	clearVotes->setGeometry(10,90,150,20);
	clearVotes->setAutoFillBackground(true); clearVotes->setPalette(QPalette(QColor(200,200,200)));

	connect(clearVotes,SIGNAL(clicked()),this,SLOT(clearVotesSlot()));

	//Manual Stimulus Control
	//Buffer a command in the stimThread during an experiment (expagent should also log this signal)
	tempLabel = new QLabel("Manual Stimuli",this);
	tempLabel->setGeometry(10,125,200,20);
	tempLabel->setFont(fnt);

	tempLabel = new QLabel("Amplitude (%):",this);
	tempLabel->setGeometry(10,145,200,20);
	ampBox = new QSpinBox(this);
	ampBox->setGeometry(100,145,60,20);
	ampBox->setMinimum(1); ampBox->setMaximum(100);
	ampBox->setValue(100); 

	tempLabel = new QLabel("Duty Cycle (%):",this);
	tempLabel->setGeometry(10,165,200,20);
	dutyBox = new QSpinBox(this);
	dutyBox->setGeometry(100,165,60,20);
	dutyBox->setMinimum(1); dutyBox->setMaximum(100);
	dutyBox->setValue(100); 

	tempLabel = new QLabel("Period (ms):",this);
	tempLabel->setGeometry(10,185,200,20);
	periodBox = new QSpinBox(this);
	periodBox->setGeometry(100,185,60,20);
	periodBox->setMinimum(1); periodBox->setMaximum(255);
	periodBox->setValue(10); 

	tempLabel = new QLabel("# Pulses:",this);
	tempLabel->setGeometry(10,205,200,20);
	nPulseBox = new QSpinBox(this);
	nPulseBox->setGeometry(100,205,60,20);
	nPulseBox->setMinimum(1); nPulseBox->setMaximum(50);
	nPulseBox->setValue(1);

	stimButton = new QPushButton("Stimulate",this);
	stimButton->setGeometry(180,165,120,40);
	fnt.setPointSize(16); stimButton->setFont(fnt);
	stimButton->setAutoFillBackground(true); stimButton->setPalette(QPalette(QColor(200,200,0)));

	connect(stimButton,SIGNAL(clicked()),this,SLOT(stimulate()));

};

ManualControls::~ManualControls(){};

void ManualControls::paintEvent(QPaintEvent *){

	QPainter painter(this);

	

};

void ManualControls::crosshairChange(){
	gui->canvas->crosshair = crosshairCheck->isChecked();
};
void ManualControls::historyChange(int val){
	
	int frames = (dataHistoryLength->value()*1000)/gui->frameIntIn->value();
	for(int i=0; i<gui->plots.size(); i++) gui->plots[i]->historyDepth = frames;
	gui->perfGraph->historyDepth = frames;

};
void ManualControls::clearVotesSlot(){
	if(!gui->expAgent) return;

	gui->expAgent->analysisMod->votesHT[0] = 0;
	gui->expAgent->analysisMod->votesHT[1] = 0;
	
}
void ManualControls::stimulate(){
	if(!gui->expAgent) return;

	gui->stimThread->queueManualStim(ampBox->value(),dutyBox->value(),nPulseBox->value(),periodBox->value());

};