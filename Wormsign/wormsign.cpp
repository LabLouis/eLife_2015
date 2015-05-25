#include "wormsign.h"
#include "stageThread.h"
#include "stimThread.h"
#include "performanceGraph.h"
#include "experimentAgent.h"
#include "dataplot.h"
#include "arenaTrack.h"
#include "behaviorModeView.h"
#include "manualControls.h"

Wormsign::Wormsign(QWidget *parent, Qt::WFlags flags)
	: QWidget(parent, flags)
{

	appVersion = "2.0";


	//Populate Camera Options
	QMainWindow * mainWin = (QMainWindow *)this->window();
	mainWin->setWindowTitle("Wormsign " + appVersion + " - RealTime Larva Tracking - Questions: Gus Lott x4632");

	expAgent = NULL;

	int height = 1000;
	int width = 1000;
	setGeometry(50,50,width,height);
	setMinimumHeight(height); setMaximumHeight(height);
	setMinimumWidth(width); setMaximumWidth(width);
	//setAutoFillBackground(true);
	//setPalette(QColor(250,250,200));

	//File Menu saves/loads config states.
	

	//Create Preview Canvas for Camera
	camThread = NULL;
	canvas = new CamViewGL;
	canvas->gui = this;
	canvas->show();

	//HARDWARE INTERFACES
	//button to show/hide hardware interfaces
	showHideHW = new QPushButton("<< Hide Config",this);
	showHideHW->setGeometry(530,10,110,20); showHideHW->setCheckable(true);
	showHideHW->setPalette(QColor(200,200,200)); showHideHW->setChecked(true);
	connect(showHideHW,SIGNAL(clicked()),this,SLOT(hideHW()));

	//Create Camera Control Interface
		QStringList camStrList = CoreCamThread::listCams();

		QGroupBox * camControlBox = new QGroupBox("Camera Control",this);
		QFont fnt = camControlBox->font(); fnt.setBold(true); camControlBox->setFont(fnt);
		camControlBox->setGeometry(645,10,350,200);
		
		//pop-up list of available cameras
		camList = new QComboBox(camControlBox);
		camList->addItems(camStrList);
		camList->setGeometry(10,20,200,20);

		//connect toggle button
		camConnect = new QPushButton("Connect",camControlBox);
		camConnect->setGeometry(220,20,100,20);
		camConnect->setCheckable(true); camConnect->setPalette(QPalette(QColor(200,200,200)));
		connect(camConnect,SIGNAL(clicked()),this,SLOT(connectCamera()));

		//ROI, Brightness, Gain, Gamma, Shutter - disabled
		QLabel * roiLabel = new QLabel("ROI (xywh):",camControlBox);
		roiLabel->setFont(fnt);
		roiLabel->setGeometry(10,50,70,20);
		for(int i=0; i<4; i++){
			roiBox[i] = new QSpinBox(camControlBox);
			roiBox[i]->setGeometry(10+80+ 65*i ,50,55,20);
			roiBox[i]->setEnabled(false);
		}
		
		QLabel * tempLabel;
		//Brightness
		tempLabel = new QLabel("Brightness: ",camControlBox); 
		brightBox = new QSpinBox(camControlBox); brightBox->setEnabled(false); 
		brightSlider = new QSlider(Qt::Horizontal,camControlBox); brightSlider->setEnabled(false);
		tempLabel->setGeometry(10,90,120,20); brightBox->setGeometry(110,90,70,20); brightSlider->setGeometry(190,90,150,20);
		//Link slider/box and connect prop update
		connect(brightBox,SIGNAL(valueChanged(int)),brightSlider,SLOT(setValue(int)));
		connect(brightSlider,SIGNAL(valueChanged(int)),brightBox,SLOT(setValue(int)));

		//Gain
		tempLabel = new QLabel("Gain: ",camControlBox); 
		gainBox = new QSpinBox(camControlBox); gainBox->setEnabled(false);
		gainSlider = new QSlider(Qt::Horizontal,camControlBox); gainSlider->setEnabled(false);
		tempLabel->setGeometry(10,110,120,20); gainBox->setGeometry(110,110,70,20); gainSlider->setGeometry(190,110,150,20);
		//Link slider/box and connect prop update
		connect(gainBox,SIGNAL(valueChanged(int)),gainSlider,SLOT(setValue(int)));
		connect(gainSlider,SIGNAL(valueChanged(int)),gainBox,SLOT(setValue(int)));
		
		//Gamma
		tempLabel = new QLabel("Gamma: ",camControlBox); 
		gammaBox = new QSpinBox(camControlBox); gammaBox->setEnabled(false);
		gammaSlider = new QSlider(Qt::Horizontal,camControlBox); gammaSlider->setEnabled(false);
		tempLabel->setGeometry(10,130,120,20); gammaBox->setGeometry(110,130,70,20); gammaSlider->setGeometry(190,130,150,20);
		//Link slider/box and connect prop update
		connect(gammaBox,SIGNAL(valueChanged(int)),gammaSlider,SLOT(setValue(int)));
		connect(gammaSlider,SIGNAL(valueChanged(int)),gammaBox,SLOT(setValue(int)));
				
		//Shutter
		shutLabel = new QLabel("Shutter (x ms): ",camControlBox); 
		shutterBox = new QSpinBox(camControlBox); shutterBox->setEnabled(false);
		shutterSlider = new QSlider(Qt::Horizontal,camControlBox); shutterSlider->setEnabled(false);
		shutLabel->setGeometry(10,150,120,20); shutterBox->setGeometry(110,150,70,20); shutterSlider->setGeometry(190,150,150,20);
		//Link slider/box and connect prop update
		connect(shutterBox,SIGNAL(valueChanged(int)),shutterSlider,SLOT(setValue(int)));
		connect(shutterSlider,SIGNAL(valueChanged(int)),shutterBox,SLOT(setValue(int)));
		
		frameInt = new QLabel("Measured Frame Interval: ",camControlBox);
		frameInt->setGeometry(10,175,200,20);
		QPalette palette = frameInt->palette();
		palette.setColor(frameInt->foregroundRole(), QColor(200,0,0));
		frameInt->setPalette(palette);

			
	//Create Network Connection Control interface, to biorules app
		//Connection to Eric Trautman's Biorules Application
		biorulesConnection = new BiorulesConnect();

		//Panel
		QGroupBox * netPanel = new QGroupBox("BioRules Network Connection",this);
		netPanel->setFont(fnt);
		netPanel->setGeometry(645,230,350,70);

		//IP/Port Controls & Connect Button
		tempLabel = new QLabel("IP:",netPanel);
		tempLabel->setGeometry(10,20,30,20);
		ipText = new QLineEdit(biorulesConnection->ip,netPanel);
		ipText->setGeometry(40,20,150,20);

		tempLabel = new QLabel("Port:",netPanel);
		tempLabel->setGeometry(10,40,40,20);
		portText = new QLineEdit(QString().sprintf("%d",biorulesConnection->port),netPanel);
		portText->setGeometry(40,40,50,20); portText->setEnabled(false);

		netConnect = new QPushButton("Connect",netPanel);
		netConnect->setGeometry(220,20,100,20); netConnect->setFont(fnt);
		netConnect->setCheckable(true); netConnect->setPalette(QColor(200,200,200));
		
		connect(netConnect,SIGNAL(clicked()),this,SLOT(connectBioRules()));
	
		

	//Create Stage Control Serial Connection Interface
		stageThread = new StageThread(this);
		stageThread->biorulesConnection = biorulesConnection;
		connect(stageThread,SIGNAL(posUpdate()),this,SLOT(update()));

		//Panel
		QGroupBox * stagePanel = new QGroupBox("Zaber Stage Serial Interface",this);
		stagePanel->setFont(fnt);
		stagePanel->setGeometry(645,310,350,120);

		tempLabel = new QLabel("Port:",stagePanel);
		tempLabel->setGeometry(10,20,30,20);
		
		stageComList = new QComboBox(stagePanel);
		QStringList comStrList = stageThread->enumeratePorts();  //Scan for available serial ports
		stageComList->insertItems(0,comStrList);
		for(int i=0; i<comStrList.size(); i++)
			if(comStrList[i] == stageThread->comName) stageComList->setCurrentIndex(i);
		stageComList->setGeometry(40,20,100,20);

		stageConnect = new QPushButton("Connect",stagePanel);
		stageConnect->setGeometry(220,20,100,20); stageConnect->setFont(fnt);
		stageConnect->setCheckable(true); stageConnect->setPalette(QColor(200,200,200));
		
		connect(stageConnect,SIGNAL(clicked()),this,SLOT(connectStage()));

		stageX = new QLabel(stagePanel);
		stageX->setGeometry(10,45,150,20);
		stageY = new QLabel(stagePanel);
		stageY->setGeometry(170,45,150,20);

		tempLabel = new QLabel("Velocity (0-4800): ",stagePanel);
		tempLabel->setGeometry(10,70,120,20);
		stageVel = new QSpinBox(stagePanel);
		stageVel->setGeometry(130,70,100,20); stageVel->setEnabled(false); stageVel->setValue(0);
		stageVel->setMinimum(0); stageVel->setMaximum(10000);

		tempLabel = new QLabel("Acceleration: ",stagePanel);
		tempLabel->setGeometry(10,90,120,20);
		stageAccel = new QSpinBox(stagePanel);
		stageAccel->setGeometry(130,90,100,20); stageAccel->setEnabled(false); stageAccel->setValue(0);
		stageAccel->setMinimum(0); stageAccel->setMaximum(10000);

	//Create Stimulus Control Serial Connection Interface, frame rate, intensity, manual stim

		stimThread = new StimThread(this);
		//Panel
		QGroupBox * stimPanel = new QGroupBox("PhotoStim + Trigger System",this);
		stimPanel->setFont(fnt);
		stimPanel->setGeometry(645,440,350,50);

		tempLabel = new QLabel("Port:",stimPanel);
		tempLabel->setGeometry(10,20,30,20);
		
		stimComList = new QComboBox(stimPanel);
		stimComList->insertItems(0,comStrList);
		for(int i=0; i<comStrList.size(); i++)
			if(comStrList[i] == stimThread->comName) stimComList->setCurrentIndex(i);
		stimComList->setGeometry(40,20,100,20);

		stimConnect = new QPushButton("Connect",stimPanel);
		stimConnect->setGeometry(220,20,100,20); stimConnect->setFont(fnt);
		stimConnect->setCheckable(true); stimConnect->setPalette(QColor(200,200,200));
		
		connect(stimConnect,SIGNAL(clicked()),this,SLOT(connectStim()));
		
		

		

	//Experiment Controls, start/stop, log, file names
		QGroupBox * expPanel = new QGroupBox("Experiment Control",this);
		expPanel->setFont(fnt);
		expPanel->setGeometry(645,500,350,160);

		fnt.setBold(false);
		dirSelect = new QPushButton("Browse...",expPanel);
		dirSelect->setGeometry(10,20,70,20); dirSelect->setFont(fnt);

		connect(dirSelect,SIGNAL(clicked()),this,SLOT(selectDir()));

		dirOpen = new QPushButton("Open Dir",expPanel);
		dirOpen->setGeometry(10,45,70,20); dirOpen->setFont(fnt);

		connect(dirOpen,SIGNAL(clicked()),this,SLOT(openDir()));


		expLoc.setFile("C:\\Data\\");
		
		expDirectory = new QLabel(expLoc.path(),expPanel);
		expDirectory->setGeometry(85,20,330,20); 
		expDirectory->setFont(fnt); 
		
		dataLog = new QCheckBox("Log Analysis Results and Stimuli",expPanel);
		dataLog->setGeometry(85,45,200,20); dataLog->setChecked(true);
		vidLog = new QCheckBox("Log Grayscale Video",expPanel);
		vidLog->setGeometry(85,65,200,20); vidLog->setChecked(true);
		markupLog = new QCheckBox("Log Video with Annotation",expPanel);
		markupLog->setGeometry(85,85,200,20); markupLog->setChecked(false);


		//list for available rules (including none) - from biorulesConnection
		tempLabel = new QLabel("BioRule:",expPanel);
		tempLabel->setGeometry(10,110,75,20);
		bioRuleList = new QComboBox(expPanel);
		bioRuleList->setGeometry(85,110,200,20);
		//bioRuleList->insertItem(0,QString("NONE"));
		
		tempLabel = new QLabel("Frame Trigger Interval (ms):",expPanel);
		tempLabel->setGeometry(10,135,180,20);
		frameIntIn = new QSpinBox(expPanel);
		frameIntIn->setGeometry(180,135,50,20); frameIntIn->setValue(33);
		frameIntIn->setRange(10,50);

		fnt.setBold(true);

	//Start/Stop Button and Experiment Ellapsed Time
	fnt.setPointSize(17);
	startStop = new QPushButton("START",this);
	startStop->setGeometry(500,950,120,40); startStop->setCheckable(true); startStop->setEnabled(false);
	startStop->setFont(fnt); startStop->setAutoFillBackground(true); startStop->setPalette(QPalette(QColor(200,0,0)));
	expTime = new QLabel("0:00:00",this);
	expTime->setGeometry(400,950,100,40);
	expTime->setFont(fnt);

	connect(startStop,SIGNAL(clicked()),this,SLOT(startStopExperiment()));


	//DATA PLOTS

	//Algorithm Performance Widget
	perfGraph = new PerformanceGraph(this);
	perfGraph->setGeometry(645,670,350,320);

	//Data Visualization - widget drawing plots of results 
	plots.resize(4);
	for(int i=0; i<4; i++){
		plots[i] = new DataPlot(this);
		plots[i]->setGeometry(10,20+240*i,300,230);
		plots[i]->parameterList->setCurrentIndex(i);
	}

	//Global Arena Trajectory - widget drawing history of animal track - clear history button
	arenaTrack = new ArenaTrack(this);
	arenaTrack->setGeometry(320,40,310,300);
	connect(bioRuleList,SIGNAL(currentIndexChanged(int)),arenaTrack,SLOT(update()));

	//Behavior mode visualization (i.e. stopped, forward, back, casting left, etc)
	behaviorModeView = new BehaviorModeView(this);
	behaviorModeView->setGeometry(320,360,310,300);

	//Manual controls
	manualControls = new ManualControls(this);
	manualControls->setGeometry(320,680,310,250);

	//check for default config in pwd and load it
}

Wormsign::~Wormsign(){};
void Wormsign::closeEvent(QCloseEvent* event){
	qApp->quit();
};

void Wormsign::hideHW(){
	if(showHideHW->isChecked()){
		int height = 1000;
		int width = 1000;
		setMinimumHeight(height); setMaximumHeight(height);
		setMinimumWidth(width); setMaximumWidth(width);
		showHideHW->setText(QString("<< Hide Config"));

	}else{
		int height = 1000;
		int width = 645;
		setMinimumHeight(height); setMaximumHeight(height);
		setMinimumWidth(width); setMaximumWidth(width);
		showHideHW->setText(QString(">> Show Config"));
	}
};

void Wormsign::connectCamera(){
	
	if(camConnect->isChecked()){
		camConnect->setText("Disconnect");
		camList->setEnabled(false);

		//Spawn acquire thread, targeting selected camera
		camThread = new CoreCamThread(camList->currentIndex(),this);
		//camThread->biorules = biorulesConnection;
		connect(camThread,SIGNAL(frameCaptured()),this,SLOT(update()));
		connect(camThread,SIGNAL(frameCaptured()),perfGraph,SLOT(update()));
		for(int i=0; i<4; i++)
			connect(camThread,SIGNAL(frameCaptured()),plots[i],SLOT(update()));
		
		connect(camThread,SIGNAL(frameCaptured()),arenaTrack,SLOT(update()));
		connect(camThread,SIGNAL(frameCaptured()),behaviorModeView,SLOT(update()));

		//Set ROI limits
		int imgX,imgY;
		camThread->getRoiLimits(&imgX, &imgY);
		roiBox[0]->setMinimum(0); roiBox[0]->setMaximum(imgX-2);
		roiBox[1]->setMinimum(0); roiBox[1]->setMaximum(imgY-2);
		roiBox[2]->setMinimum(1); roiBox[2]->setMaximum(imgX);
		roiBox[3]->setMinimum(1); roiBox[3]->setMaximum(imgY);

		//Set Property Limits
		int max,min;
		camThread->getBrightRange(&min,&max);
		brightBox->setRange(min,max);
		brightSlider->setRange(min,max);
		
		camThread->getGainRange(&min,&max);
		gainBox->setRange(min,max);
		gainSlider->setRange(min,max);

		camThread->getGammaRange(&min,&max);
		gammaBox->setRange(min,max);
		gammaSlider->setRange(min,max);

		camThread->getShutterRange(&min,&max);
		shutterBox->setRange(min,max);
		shutterSlider->setRange(min,max);

		//Enable cam prop UI elements
		brightBox->setEnabled(true); gainBox->setEnabled(true);
		gammaBox->setEnabled(true); shutterBox->setEnabled(true);
		brightSlider->setEnabled(true); gainSlider->setEnabled(true);
		gammaSlider->setEnabled(true); shutterSlider->setEnabled(true);
		for(int i=0; i<4; i++) roiBox[i]->setEnabled(true);

		//update values
		refreshCamProps();
		//connect signals
		connect(brightBox,SIGNAL(valueChanged(int)),this,SLOT(setCamProps()));
		connect(gainBox,SIGNAL(valueChanged(int)),this,SLOT(setCamProps()));
		connect(gammaBox,SIGNAL(valueChanged(int)),this,SLOT(setCamProps()));
		connect(shutterBox,SIGNAL(valueChanged(int)),this,SLOT(setCamProps()));

		for(int i=0; i<4; i++) connect(roiBox[i],SIGNAL(editingFinished()),this,SLOT(setCamProps()));

		connect(camThread,SIGNAL(frameCaptured()),canvas,SLOT(update()));

		camThread->start();
	
	}else{
		camThread->stop();
		delete camThread;
		camThread = NULL;

		camConnect->setText("Connect");
		//Enable cam prop UI elements
		brightBox->setEnabled(false); gainBox->setEnabled(false);
		gammaBox->setEnabled(false); shutterBox->setEnabled(false);
		brightSlider->setEnabled(false); gainSlider->setEnabled(false);
		gammaSlider->setEnabled(false); shutterSlider->setEnabled(false);
		for(int i=0; i<4; i++) roiBox[i]->setEnabled(false);

		camList->setEnabled(true);

	}

	checkStartable();

	
};

void Wormsign::setCamProps(){

	//Set all props from UI widget values
	unsigned short br, gn, gm, sh;
	br = brightBox->value();
	gn = gainBox->value();
	gm = gammaBox->value();
	sh = shutterBox->value();
	camThread->setProps(br,gn,gm,sh);

	unsigned short x,y,w,h;
	x = roiBox[0]->value();
	y = roiBox[1]->value();
	w = roiBox[2]->value();
	h = roiBox[3]->value();
	camThread->setRoi(x,y,w,h);

	//refreshCamProps();

};
void Wormsign::refreshCamProps(){

	//update roi values
	camThread->updateRoi();
	roiBox[0]->setValue(camThread->left); roiBox[1]->setValue(camThread->top);
	roiBox[2]->setValue(camThread->width); roiBox[3]->setValue(camThread->height);

	//Update props
	camThread->updateProps();
	brightBox->setValue(camThread->brightness);
	gainBox->setValue(camThread->gain); 
	gammaBox->setValue(camThread->gamma);
	shutterBox->setValue(camThread->shutter);

	//Value true for 20us increment ticks (Basler A622f)
	shutLabel->setText(QString().sprintf("Shutter: (%.1fms)",double(camThread->shutter)*.02));

}


//Network Connection To BioRules App
void Wormsign::connectBioRules(){
	if(netConnect->isChecked()){
		//Connect to the given IP
		biorulesConnection->ip = ipText->text();
		biorulesConnection->port = portText->text().toInt();
		ipText->setEnabled(false);
		if(!biorulesConnection->connectToServer()){
			netConnect->setChecked(false);
			ipText->setEnabled(true);
			return;
		}
		
		//Disable UI
		netConnect->setText("Disconnect");

		//Query available rules
		QStringList cfgs = biorulesConnection->listConfigs();
		//Server connection will be reset later, so disconnect for now
		biorulesConnection->disconnectFromServer();
		
		//Populate rules list UI
		bioRuleList->clear();
		bioRuleList->insertItems(0,cfgs);

		arenaTrack->update();

	}else{
		//disconnect
		netConnect->setText("Connect");
		ipText->setEnabled(true);

		bioRuleList->clear();
	}
	checkStartable();
};

void Wormsign::connectStage(){

	if(stageConnect->isChecked()){
		//Connect (start polling thread)
		stageConnect->setText("Disconnect");
		stageComList->setEnabled(false);
		stageVel->setEnabled(false);
		stageAccel->setEnabled(false);

		//if values haven't been read yet, read them out
		if(stageVel->value() != 0){
			stageThread->vel = stageVel->value();
			stageThread->accel = stageAccel->value();
		}

		stageThread->comName = stageComList->currentText();
		stageThread->state = StageThread::POLLPOSOTION;
		stageThread->start();

		while(stageThread->stopped) Sleep(1);

		stageVel->setValue(stageThread->vel);
		stageAccel->setValue(stageThread->accel);


	}else{
		stageThread->stop();
		stageConnect->setText("Connect");
		stageComList->setEnabled(true);	
		stageVel->setEnabled(true);
		stageAccel->setEnabled(true);
	}
	checkStartable();

};

void Wormsign::connectStim(){

	if(stimConnect->isChecked()){
		stimConnect->setText("Disconnect");
		stimComList->setEnabled(false);
		stimThread->comName = stimComList->currentText();
		stimThread->connectCOM();
		stimThread->sendStopSignalCOM();
		stimThread->disconnectCOM();
	}else{
		stimConnect->setText("Connect");
		stimComList->setEnabled(true);
	}
	checkStartable();
}

void Wormsign::selectDir(){

	QString dir = QDir::toNativeSeparators(QFileDialog::getExistingDirectory(this,QString("Select Experiment Directory"),expLoc.path()));
	if (dir.isEmpty()) return;
	expLoc.setFile(dir);
	
	expDirectory->setText(dir);

};

void Wormsign::openDir(){

	QStringList args;
	args << QDir::toNativeSeparators(expDirectory->text());
	QProcess::execute("explorer",args);

};

//Determine if experiment can be started (enough hardware is connected)
void Wormsign::checkStartable(){
	
	if(stimConnect->isChecked() & stageConnect->isChecked() & camConnect->isChecked() & netConnect->isChecked())
		startStop->setEnabled(true);
	else
		startStop->setEnabled(false);


};

void Wormsign::startStopExperiment(){
	if(startStop->isChecked()){
		//Start Experiment

		//Disable UI
		for(int i=0; i<4; i++) roiBox[i]->setEnabled(false);
		brightBox->setEnabled(false); brightSlider->setEnabled(false);
		gainBox->setEnabled(false); gainSlider->setEnabled(false);
		gammaBox->setEnabled(false); gammaSlider->setEnabled(false);
		shutterBox->setEnabled(false); shutterSlider->setEnabled(false);
		camConnect->setEnabled(false);
		
		netConnect->setEnabled(false);
		stageConnect->setEnabled(false);
		stimConnect->setEnabled(false);

		dirSelect->setEnabled(false);
		//vidLog->setEnabled(false);
		//dataLog->setEnabled(false);
		bioRuleList->setEnabled(false);
		frameIntIn->setEnabled(false);

		expTime->setText("0:00:00");

		startStop->setText("STOP");
		startStop->setPalette(QPalette(QColor(0,200,0)));

		//Launch experiment agent that monitors progress and lazily logs data
		ExperimentAgent * tAgent = new ExperimentAgent(this);
		expAgent = tAgent;
		

		connect(tAgent,SIGNAL(errorDetected()),this,SLOT(stopOnError()));


	}else{
		//Stop Experiment
		qDebug("STOPPED EXPERIMENT");
		//stop experiment agent
		expAgent->stop();
		delete expAgent;
		expAgent = NULL;

		//Re-enable GUI
		for(int i=0; i<4; i++) roiBox[i]->setEnabled(true);
		brightBox->setEnabled(true); brightSlider->setEnabled(true);
		gainBox->setEnabled(true); gainSlider->setEnabled(true);
		gammaBox->setEnabled(true); gammaSlider->setEnabled(true);
		shutterBox->setEnabled(true); shutterSlider->setEnabled(true);
		camConnect->setEnabled(true);
		
		netConnect->setEnabled(true);
		stageConnect->setEnabled(true);
		stimConnect->setEnabled(true);

		dirSelect->setEnabled(true);
		//vidLog->setEnabled(true);
		//dataLog->setEnabled(true);
		bioRuleList->setEnabled(true);
		frameIntIn->setEnabled(true);

		startStop->setText("START");
		startStop->setPalette(QPalette(QColor(200,0,0)));

	}
};

void Wormsign::paintEvent(QPaintEvent*){

	if(camThread)
		frameInt->setText(QString().sprintf("Measured Frame Interval: %.1f ms",camThread->frameInterval+camThread->analysisInterval));

	if(expAgent)
		expTime->setText(QString().sprintf("%d:%02d:%02d",expAgent->hrs,expAgent->mins,expAgent->secs));

	if(stageThread){
		stageX->setText(QString().sprintf("x: %.3f mm",double(stageThread->xposMM)));
		stageY->setText(QString().sprintf("y: %.3f mm",double(stageThread->yposMM)));
		//stageX->setText(QString().sprintf("x: %d usteps",stageThread->xpos));
		//stageY->setText(QString().sprintf("y: %d usteps",stageThread->ypos));
	}
		

};

void Wormsign::stopOnError(){
	startStop->setChecked(false);
	startStopExperiment();
	QMessageBox::critical(NULL,"BIORULES ERROR",stimThread->errorCode + ": " + stimThread->errorMessage);
}