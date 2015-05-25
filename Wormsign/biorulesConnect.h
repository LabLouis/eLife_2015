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
	BIORULESCONNECT.H - Network Connection to Venkman Biorules server
	Interface to Eric Trautman's Biorules Server
		This object utilized by the serial thread that is monitoring for stimuli
		when that thread is not running, direct calls to this object will return details about rules

		Manages the communication protocol between the analysis results and the biorules responses
			- Connect to server and list available configurations
			- Read back olfactory landscape images from biorules server for display in UI
			- Create a session implementing a biorule

		Stimulus thread accesses the TCP socket directly with its own methods constructing packets based
		upon the agreed upon format for communications between this client and the venkman server

*/
#ifndef BIORULESCONNECT_H
#define BIORULESCONNECT_H

#include <QtCore>
#include <QTcpSocket>
#include <windows.h>
#include <opencv2/opencv.hpp>

class BiorulesConnect : public QObject
{
	Q_OBJECT

public:
	BiorulesConnect();
	~BiorulesConnect();

	QString bioRulesVersion;
	QString messageVersion;

	//Network Interface - sending bytes of serial data over TCP/IP
	QTcpSocket * socket;
	
	//Connection methods
	QString ip;
	int port;
	bool connectToServer();
	void disconnectFromServer();
	
	//Venkman session handling functions
	bool openSession(QString session);
	QString sessionId;
	char sessionIdChar[512];
	bool closeSession();

	QStringList readResponse();
	
	//Blocking Communications
	char * inData; //input buffer
	int inDataSize;
	QStringList listConfigs();  //List available biorules
	QStringList configList;

	//for extracting comma separated fields from any response
	QStringList getFields(char * data);  

	//Get field images (if any) associated with the config list
	std::vector<cv::Mat> fieldImages;	
	void getFieldImage(QString configName, cv::Mat & img);

	//Get Current rule parameters
	void getRuleParameters();
	QString paramResponse;

};

#endif // BIORULESCONNECT_H
