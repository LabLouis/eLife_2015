/*
	Interface to Lakshmi Ramasamy's Stimulus and camera trigger generation unit

*/
#ifndef STIMTHREAD_H
#define STIMTHREAD_H

#include <QtGui>
#include <QtCore>
#include <QThread>
#include <QTcpSocket>
#include <windows.h>
#include "biorulesConnect.h"
#include "wormsign.h"

class StimThread : public QThread
{
	Q_OBJECT

public:
	StimThread(Wormsign * ui);
	~StimThread();

	Wormsign * gui;

	QString stimVersion;
		
	//Serial Interface
	HANDLE hCom;
	QString comName;
	DWORD txBytes, rxBytes; 
	bool connectCOM();
	bool disconnectCOM();
	int readTimeResponseCOM();
	void sendStartSignalCOM(int frameIntMS);
	void sendStopSignalCOM();

	//to track analysis
	int index;
	void makeResultPacket(int ind, char * buffer);
	std::vector<double> stimAmps; //for plotting

	QString makeStimPacket(QStringList & response);

	//Manual stimulation
	void queueManualStim(int pAmp, int pDuty, int nRepeat, int period);
	bool stimQueued;
	QString qStim;
	int qAmp;

	//Profile communication time
	double netDiff, stimDiff;

	bool errorExit;
	QString errorCode, errorMessage;
	
	//For reading back response from server containing session parameters
	bool logParameters;

	bool exited, stopped;
	void run();
	void stop();
	

};

#endif // STIMTHREAD_H
