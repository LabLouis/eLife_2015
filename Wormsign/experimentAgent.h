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
	EXPERIMENTAGENT.H - Experiment Agent
		Composes and executes the experiment
		Monitors status
		Coordinates, Wraps up, and closes Experiment
		Spawns a thread that conducts all logging
			- Video
			- Analysis Results
			- Stimuli
			- Config
		Contains the Analysis Module which is called from the camera thread
*/

#ifndef EXPERIMENTAGENT_H
#define EXPERIMENTAGENT_H

#include <QtCore>
#include <windows.h>
#include "wormsign.h"
#include "analysisModule.h"
#include <opencv2/opencv.hpp>
class VidLogThread;

class ExperimentAgent : public QThread
{
	Q_OBJECT

public:
	ExperimentAgent(Wormsign * ui, bool log = true);
	~ExperimentAgent();

	Wormsign * gui;

	QDateTime dateTime;
	QString froot;
	QFile dataFile; //stores full results/biorules response packets per frame
	QTextStream dataStream;
	QFile configFile; //stores the experiment configuration data
	QFile dataLegend; //data legend
	QTextStream otherStream;

	VidLogThread * vidLogger;
	VidLogThread * markupLogger;
	
	bool logAnnotate;
	bool logVid;
	bool logData;

	AnalysisModule * analysisMod;

	//Experiment Control Functions
	void initExperiment();
	void closeExperiment();
	QString createConfigString();

	int index;
	void updateTime();
	int totalsecs,secs,mins,hrs;

	//Logging & Monitoring thread
	bool exited, stopped, errorEmitted;
	void run();
	void stop();

	cv::Mat makeFieldImage();

signals:
	void errorDetected();

};

#endif //EXPERIMENTAGENT_H