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
	ARENATRACK.H - Arena Track View
		Plots the global position over the arena for the entire experiment
		 Uses 1 point per 30 frames of video
		Covers the entire 400x400mm surface

*/
#ifndef ARENATRACK_H
#define ARENATRACK_H

#include <QtGui>
#include <QtCore>
#include "wormsign.h"

class ArenaTrack : public QWidget
{
	Q_OBJECT

public:
	ArenaTrack(Wormsign * ui);
	~ArenaTrack();
	
	//Reference to application elements
	Wormsign * gui;

	QPolygonF plotData;  //Polygon to plot data

	void paintEvent(QPaintEvent *);  //for custom drawing in the QWidget
	
};

#endif // ARENATRACK_H
