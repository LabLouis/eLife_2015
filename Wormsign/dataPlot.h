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
	DATAPLOT.H - Data visualization for analysis results
	Data Plot
		Display a parameter in time
		Display stimulus in time
		Variable selected from a drop down menu in this plot

		Uses a circular buffer to plot values

*/
#ifndef DATAPLOT_H
#define DATAPLOT_H

#include <QtGui>
#include "wormsign.h"

class DataPlot : public QWidget
{
	Q_OBJECT

public:
	DataPlot(Wormsign * ui);
	~DataPlot();

	//reference to application structure (to pull data from analysis object)
	Wormsign * gui;

	//Filled with datavalues and stimulus values versus time
	QPolygonF plotData;
	QPolygonF stimData;
	int historyDepth; //width of plot

	QComboBox * parameterList; //drop down list of data parameters to plot

	void paintEvent(QPaintEvent *);  //custom drawing event
	
};

#endif // DATAPLOT_H
