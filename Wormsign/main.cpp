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
	MAIN.CPP - Main Application Entry Point
		This function launches the application GUI (defined in wormsign.h)
		and sets the windows priority class to REALTIME.  It then initalizes the
		Qt event monitoring loop (in QApplication) which handles button-presses
		and graphics draw updates, etc.

	The application is structured as several concurrent processes:
		1) Main GUI Thread - defined in wormsign.h
		2) Camera Interface Thread - captures images and does analysis
		3) Stimulus Thread - monitors camera thread for analysis results and sends stimulus
		4) Stage Thread - Communicates with Zaber stages to keep track of current stage position and to track the animal
		5) Experiment Agent Thread - Coordinates the execution of the experiment and carries out data logging

	Wormsign must interconnect with the following systems:
		1) Eric Trautman's "Venkman" biorules server application (network socket)
		2) 4 Zaber Stages configured as described in the system hardware schematic (single serial interface)
		3) Magnus Karlsson's stimulus delivery and camera triggering embedded system (single serial interface)
		4) Basler A622fm Video Camera (IEEE 1394 w/ Basler Pylon Drivers installed)

	This application relies upon the following external software libraries:
		1) Developed under Windows 7 using the windows SDK
		2) Qt v4.7 Application Development Framework
		3) OpenCV v2.2 Computer Vision Library 
		4) Basler Pylon v2.3 Camera Interface API

	Developed in Microsoft Visual Studio 2008

*/


#include "wormsign.h"
#include <QtGui/QApplication>
#include <windows.h>

int main(int argc, char *argv[])
{

	//Maximize windows process priority
	SetPriorityClass(GetCurrentProcess(),REALTIME_PRIORITY_CLASS);

	QApplication a(argc, argv);
	Wormsign w;
	w.show();
	return a.exec();
}
