/*
	Asynchronous (from the camera stream) communication with the zaber stages
		- Will deliver motion control commands to track the animal
		- Will poll stages for absolute position information

*/
#ifndef STAGETHREAD_H
#define STAGETHREAD_H

#include <QtCore>
#include <QThread>
#include <QTcpSocket>
#include <windows.h>
#include "biorulesConnect.h"
#include "wormsign.h"

class StageThread : public QThread
{
	Q_OBJECT

public:
	StageThread(Wormsign * ui);
	~StageThread();

	enum OpState { NONE, POLLPOSOTION, EXPERIMENT, HOME };

	Wormsign * gui;
	//Network Interface
	BiorulesConnect * biorulesConnection;
	
	OpState state;

	//Serial Interface
	HANDLE hCom;
	char cBuf[512];
	unsigned char ucBuf[512]; //receive buffer
	char posReq[6]; //for a command to be sent
	char oneRead[6]; //for a command to be sent
	DWORD txBytes, rxBytes; 

	QString comName;
	int index;

	QStringList enumeratePorts();
	bool connectCOM();
	bool disconnectCOM();

	//Stage Communication functions for the Zaber T-LSR450B
	void pollPosition();
	void sendRelativeMove(int dx, int dy);
	void fastPacket(int dx, int dy); //send both position/relativemove interleaved
	
	void sendHome();
	void setVelocityTarget(int vTarget);
	int readVelocityTarget();
	void setAcceleration(int accel);
	int readAcceleration();
	int vel, accel;

	int xpos, ypos;
	double xposMM, yposMM;
	double tickPerMM_X, tickPerMM_Y;

	bool exited, stopped;
	void run();
	void stop();
	
signals:
	void posUpdate();

};

#endif // STAGETHREAD_H
