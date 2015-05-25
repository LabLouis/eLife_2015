/*
	Wormsign - Closed Loop Larva Virtual-Olfaction-Reality System
		Real-Time Image Analysis with Animal Tracking, Stimulus Delivery, and Data Acquisition

	Software Developed By:
	Gus K Lott III, PhD, PMP
	lottg@janelia.hhmi.org
	April 2011
	
	Larva Tracker Project Team:
	Science: Matthieu Louis, Vivek Jayaraman, Alex Gomez-Marin, Vani Rajendran
	Engineering: Gus Lott, Eric Trautman, Lakshmi Ramasamy, Magnus Karlsson, Chris Werner, Pete Davies

	HHMI Janelia Farm Research Campus
	19700 Helix Dr.
	Ashburn, VA, USA 20147

	This File:
	WORMSIGN.H - Main User Interface widget and application structure object
		
		Primary User Interface Control Panel
		This widget displays status and control panels for the user and spawn the GL Widget for visualization of the camera and tracking stream
		It also launches and manage the application threads through the experiment agent

*/
#ifndef WORMSIGN_H
#define WORMSIGN_H

#include <QtGui>
#include <windows.h>
#include "1394Camera.h"
#include "biorulesConnect.h"
#include "coreCamThread.h"
#include "camViewGL.h"
class StageThread;
class StimThread;
class PerformanceGraph;
class ExperimentAgent;
class DataPlot;
class ArenaTrack;
class BehaviorModeView;
class ManualControls;

class Wormsign : public QWidget
{
	Q_OBJECT

public:
	Wormsign(QWidget *parent = 0, Qt::WFlags flags = 0);
	~Wormsign();

	QString appVersion;

	void closeEvent(QCloseEvent* event);

	//Camera Drawing Space, displays analysis results overlaid on the image stream
	CamViewGL * canvas;
	//Camera Thread for acquisition in and out of an experiment
	CoreCamThread * camThread;

	//Hardware Interfaces
	QPushButton * showHideHW;
	//Camera Control Interface
		QComboBox * camList;
		QPushButton * camConnect;
		QSpinBox * roiBox[4];
		QSpinBox * brightBox;  QSlider * brightSlider;
		QSpinBox * gainBox;  QSlider * gainSlider;
		QSpinBox * gammaBox;  QSlider * gammaSlider;
		QSpinBox * shutterBox;  QSlider * shutterSlider;
		QLabel * shutLabel;
		QLabel * frameInt;

	//Network Interface (TCP/IP to Venkman Biorules server)
		BiorulesConnect * biorulesConnection;
		QLineEdit * ipText;
		QLineEdit * portText;
		QPushButton * netConnect;

	//Stage Interface (Zaber stages)
		StageThread * stageThread;
		QComboBox * stageComList;
		QPushButton * stageConnect;
		QLabel * stageX, * stageY;
		QSpinBox * stageVel;
		QSpinBox * stageAccel;

	//Stimulus Interface (to Magnus' board)
		StimThread * stimThread;
		QComboBox * stimComList;
		QPushButton * stimConnect;
	
	//Experiment Controls (i.e. directory, frame interval, data to log)
		QLabel * expDirectory;
		QFileInfo expLoc;
		QPushButton * dirSelect;
		QPushButton * dirOpen;
		QCheckBox * vidLog;
		QCheckBox * dataLog;
		QCheckBox * markupLog;
		QComboBox * bioRuleList;
		QSpinBox * frameIntIn;

	//Performance Graph
		PerformanceGraph * perfGraph;

	//Experiment Start/Stop and Time Monitor
	QPushButton * startStop;
	QLabel * expTime;
	void checkStartable();  //determine if experiment can be started

	//Object that manages the synchronization and execution of an experiment
	ExperimentAgent * expAgent;

	//For plotting parameters (currently 4 in the left side of the UI)
	std::vector<DataPlot *> plots;

	//For Global Arena View
	ArenaTrack * arenaTrack;

	//For Behavior State Visualization
	BehaviorModeView * behaviorModeView;

	//Manual Controls for stimulus and head/tail flip
	ManualControls * manualControls;

//Reimplemented functions
	void paintEvent(QPaintEvent*);

public slots:
	void hideHW(); //Shrinks the controls GUI to hide the hardware interface controls

	//Callbacks for hardware interface panels
	void connectCamera(); //Connect to a Basler camera (start the camera thread)
	void setCamProps();  //Update camera properties
	void refreshCamProps(); //Refresh camera properties when a value is changed
	void connectBioRules();	 //Connect to venkman biorules
	void connectStage(); //connect to zaber stage
	void connectStim(); //Connect to Magnus' stimulation box

	//For experiment directory specification and browsing
	void selectDir();
	void openDir();

	//master start/stop button launches experiment agent
	void startStopExperiment();

	void stopOnError();

};

#endif // WORMSIGN_H
