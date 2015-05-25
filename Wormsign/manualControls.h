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
	MANUALCONTROLS.H - Controls for Head/Tail reset and Manual Stimulation
		Provide the user the ability to apply manual stimulus (stim parameters as well)
			- Queues a stimulus in the stimthread object
		Head/Tail Votes reset and assignment flip controls
		Enable/disable crosshairs in visualization window
*/
#ifndef MANUALCONTROLS_H
#define MANUALCONTROLS_H

#include <QtGui>
#include "wormsign.h"

class ManualControls : public QWidget
{
	Q_OBJECT

public:
	ManualControls(Wormsign * ui);
	~ManualControls();

	Wormsign * gui;

	QCheckBox * crosshairCheck;
	QSpinBox * dataHistoryLength;
	QPushButton * clearVotes;

	QSpinBox * ampBox;
	QSpinBox * dutyBox;
	QSpinBox * periodBox;
	QSpinBox * nPulseBox;
	QPushButton * stimButton;

	void paintEvent(QPaintEvent *);
	
public slots:
	void crosshairChange();
	void historyChange(int val);
	void clearVotesSlot();
	void stimulate();
};

#endif // MANUALCONTROLS_H
