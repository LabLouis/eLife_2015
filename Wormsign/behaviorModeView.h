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
	BEHAVIORMODEVIEW.H - Behavior Mode View
		Displays a graphical visualization of current state and transitions from state to state
		Displays a flashing light when stimulus is presented
		Draws the skeleton of the animal during acquisition

*/
#ifndef BEHAVIORMODEVIEW_H
#define BEHAVIORMODEVIEW_H

#include <QtGui>
#include "wormsign.h"

class BehaviorModeView : public QWidget
{
	Q_OBJECT

public:
	BehaviorModeView(Wormsign * ui);
	~BehaviorModeView();

	Wormsign * gui;

	void paintEvent(QPaintEvent *);
	
};

#endif // BEHAVIORMODEVIEW_H
