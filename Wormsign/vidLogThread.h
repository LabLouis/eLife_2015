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
	VIDLOGTHREAD.H - Video Logger
		Spawns a thread that logs video
		Distributes the compression task to other cores
*/

#ifndef VIDLOGTHREAD_H
#define VIDLOGTHREAD_H

#include <QtCore>
#include <windows.h>
#include "wormsign.h"
#include "experimentAgent.h"
#include "analysisModule.h"
#include <opencv2/opencv.hpp>

class VidLogThread : public QThread
{
	Q_OBJECT

public:
	VidLogThread(QString fileName, cv::Size imSize, bool annotate, AnalysisModule * aM,int nBuffs, Wormsign * ui);
	~VidLogThread();

	Wormsign * gui;

	QString fName;
	int width, height;
	cv::VideoWriter * vidLogger;
	bool logAnnotate;
	
	AnalysisModule * aMod;

	bool addFrame(uchar * im); //hand in grayscale image
	int frameCount;
	int framesLogged;
	std::vector<uchar*> imBuffer;
	int nBuffers, thisImg, latestImg;
	
	//Logging & Monitoring thread
	bool exited, stopped;
	void run();
	void stop();

	void annotateImage(cv::Mat & img,int ind);


};

#endif //VIDLOGTHREAD_H