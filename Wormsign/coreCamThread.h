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
	CORECAMTHREAD.H - High priority thread dedicated to image acquisition and analysis
	
	Thread manages:
		- Camera Configuration and Acquisition
		- Calls analysis method on incoming images during an experiment
		- Acts as the timebase for the entire application

	Achieves camera control via the Basler Pylon API and drivers

*/
#ifndef CORECAMTHREAD_H
#define CORECAMTHREAD_H

#include <QtCore>
#include <QThread>
#include <QTcpSocket>
#include <windows.h>
#include "biorulesConnect.h"
//Basler API
#include <pylon/PylonIncludes.h>
#include <pylon/1394/Pylon1394Includes.h>
#include <pylon/1394/Basler1394Camera.h>

using namespace Pylon;
using namespace GenApi;
using namespace Basler_IIDC1394CameraParams;

class Wormsign;

class CoreCamThread : public QThread
{
	Q_OBJECT

public:
	CoreCamThread(int cId,Wormsign *ui);
	~CoreCamThread();

	Wormsign * gui;

	//Camera Interface
	int camId;
	//C1394Camera cam;
	bool expActive;
	static QStringList listCams();

	//Pylon Interface
	PylonAutoInitTerm autoInitTerm;
	ITransportLayer* pTl;
	CBasler1394Camera camera;
	

	static const int nBuffers = 50;
	
	//ROI and prop getters and setters
	int left,top,width,height;
	void updateRoi();
	void getRoiLimits(int * hMax, int * vMax);
	void setRoi(unsigned short x, unsigned short y, unsigned short w, unsigned short h);
	unsigned short brightness, gain, gamma, shutter;
	void updateProps();
	void getBrightRange(int *min, int *max);
	void getGainRange(int *min, int *max);
	void getGammaRange(int *min, int *max);
	void getShutterRange(int *min, int *max);
	void setProps(unsigned short br, unsigned short gn, unsigned short gm, unsigned short sh);
	
	//Camera trigger controls
	void setTrig(bool state = true);

	//Frame Capture details
	int frameCount;
	std::vector<uchar *> imBuffer;
	int definedInterval;
	
	//profiling
	double frameInterval, analysisInterval;

	//calibration factor for camera pixels to microns
	double umPerPixel; //set in constructor

	//tracking for dropped frames - not implemented yet
	std::vector<uchar *> bTargets;
	int bPtr;
	
	//Thread Functions
	bool exited, stopped;
	void run(); //call start() to initialize thread (will automatically call run() in the thread)
	void stop();

	//Functions to synchronize the conclusion of an experiment (called externally to pause the acquisition loop)
	void pauseAcquisition();
	void resumeAcquisition();
	bool paused; bool pause;


signals:
	void frameCaptured();  //Tell GUI components that a new frame of data is available


};

#endif // CORECAMTHREAD_H
