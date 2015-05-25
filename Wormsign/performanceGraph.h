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
	PERFORMANCEGRAPH.H - Plot algorithm and total frame trigger to act closed loop time
		Display various profiled times relative to absolute closed loop time during an experiment
			- Exposure Time
			- Image Data Transfer Time
			- Image Processing Algorithm duration
			- Communication time to Trautman Biorules server (Venkman)

*/
#ifndef PERFORMANCEGRAPH_H
#define PERFORMANCEGRAPH_H

#include <QtGui>
#include "wormsign.h"

class PerformanceGraph : public QWidget
{
	Q_OBJECT

public:
	PerformanceGraph(Wormsign * ui);
	~PerformanceGraph();

	Wormsign * gui;

	QPolygonF plotData;
	QPolygonF algorithmData;
	QPolygonF networkData;
	QPolygonF exposureData;
	bool newSize;
	int historyDepth;

	void paintEvent(QPaintEvent *);
	
};

#endif // PERFORMANCEGRAPH_H
