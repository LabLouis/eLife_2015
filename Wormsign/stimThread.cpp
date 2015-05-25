#include "stimThread.h"
#include "experimentAgent.h"
#include "stageThread.h"

StimThread::StimThread(Wormsign * ui):QThread(){
	stopped = true;
	exited = true;
	stimQueued = false;

	gui = ui;

	comName = "COM5";
	stimVersion = "1";

	stimAmps.resize(2000);
	for(int i=0; i<stimAmps.size(); i++) stimAmps[i] = 0;

	errorExit = false;

};
StimThread::~StimThread(){};



void StimThread::run(){
	
	QString command;
	
	//Maximize thread priority relative to process priority
	SetThreadPriority(GetCurrentThread(),THREAD_PRIORITY_TIME_CRITICAL);

	connectCOM(); //connect to lakshmi's stimulator
	sendStopSignalCOM(); //stop any active triggers
	
	int frameInterval = gui->frameIntIn->value();
	gui->camThread->definedInterval = frameInterval;
	
	index = -1;
	int nextInd;
	char sendBuf[2048];

	LARGE_INTEGER pcTic, pcToc, pcFreq;
	QueryPerformanceFrequency(&pcFreq);
	double loopTime;
	int responseTime;
	
	QStringList response;
	
	gui->biorulesConnection->connectToServer();
	gui->biorulesConnection->openSession(gui->bioRuleList->currentText());	

	stopped = false;
	exited = false;
	errorExit = false;
	logParameters = false;


	while(!stopped){
		//monitor analysisModule for new data
		if(errorExit) continue;
		if(!gui->expAgent) continue;
		if(!gui->expAgent->analysisMod) continue;
		if(gui->expAgent->analysisMod->index != index){
			nextInd = (index + 1)%gui->expAgent->analysisMod->nBuffers;
			
			//send to biorules app
			QueryPerformanceCounter(&pcTic);
			makeResultPacket(nextInd, sendBuf);
			gui->biorulesConnection->socket->write(sendBuf);

			//get feedback from biorules application
			response = gui->biorulesConnection->readResponse();
			//If an error is detected
			if(response[2].toInt() != 200){
				qDebug("ERROR CONDITION\n Error Code: %d\nError Message: %s",response[2].toInt(),response[3].toAscii().constData());

				errorCode = response[2];
				errorMessage = response[3];
				
				//Should stop experiment and popup an error window
				errorExit = true;
				continue;
				
			}
						
			QueryPerformanceCounter(&pcToc);
			netDiff = 1000.0*double(pcToc.QuadPart - pcTic.QuadPart)/double(pcFreq.QuadPart);
			gui->expAgent->analysisMod->networkTime[nextInd] = netDiff;
			QueryPerformanceCounter(&pcTic);

			//Log classified behavior state
			if(response[4] == QString("run")) gui->expAgent->analysisMod->behaviorState[nextInd] = AnalysisModule::RUN;
			else if(response[4] == QString("back-up")) gui->expAgent->analysisMod->behaviorState[nextInd] = AnalysisModule::BACKUP;
			else if(response[4] == QString("stop")) gui->expAgent->analysisMod->behaviorState[nextInd] = AnalysisModule::STOP;
			else if(response[4] == QString("turn-right")) gui->expAgent->analysisMod->behaviorState[nextInd] = AnalysisModule::TRIGHT;
			else if(response[4] == QString("turn-left")) gui->expAgent->analysisMod->behaviorState[nextInd] = AnalysisModule::TLEFT;
			else if(response[4] == QString("cast-right")) gui->expAgent->analysisMod->behaviorState[nextInd] = AnalysisModule::CRIGHT;
			else if(response[4] == QString("cast-left")) gui->expAgent->analysisMod->behaviorState[nextInd] = AnalysisModule::CLEFT;
			else gui->expAgent->analysisMod->behaviorState[nextInd] = AnalysisModule::NONE;
			
			//Determine if stimulus must be sent
				
				if(stimQueued){
					//If a manual stimulus is queued, send it
					//overrides any other command from the rules server
					command = qStim;
					stimAmps[(index+1)%stimAmps.size()] = qAmp;
					stimQueued = false;
				}else{
					//process any response from the biorules app
					if(response.size() > 5){
						//Form stimulus command
						command = makeStimPacket(response);
						stimAmps[(index+1)%stimAmps.size()] = response[5].toDouble();
					}else{
						//just request time feedback
						command = QString("time\n");
						stimAmps[(index+1)%stimAmps.size()] = 0;
					}
				}

				//qDebug() << command;
				WriteFile(hCom,command.toAscii().constData(),command.size(),&txBytes,NULL);
				
				//read back timestamp
				responseTime = readTimeResponseCOM();
				loopTime = (double(responseTime)/10.0 - double(gui->expAgent->analysisMod->sampleTime[nextInd]));
				
				gui->expAgent->analysisMod->loopTime[nextInd] = loopTime;
								
				//log stimulus type
				gui->expAgent->analysisMod->stimCode[nextInd] = command;
				//log stimulus amplitude history
				gui->expAgent->analysisMod->stimAmps[nextInd] = stimAmps[(index+1)%stimAmps.size()];
				if(gui->expAgent->analysisMod->stimAmps[nextInd] > gui->expAgent->analysisMod->binStimMax) 
					gui->expAgent->analysisMod->binStimMax = gui->expAgent->analysisMod->stimAmps[nextInd];

				QueryPerformanceCounter(&pcToc);
				stimDiff = 1000.0*double(pcToc.QuadPart - pcTic.QuadPart)/double(pcFreq.QuadPart);

			index++;
		}else{
			//wait for analysis to complete on next frame
			QThread::usleep(1); 
			//This sleep adds jitter to timebase, but without it, 
			//	this thread eats an entire core on the PC in this empty while loop
			//  Dedicating an entire thread to this process is not a problem on this multi-core system
		}
	}


	//Send stop command
	sendStopSignalCOM();
	disconnectCOM();

	if(logParameters){
		//Capture biorules configuration
		gui->biorulesConnection->getRuleParameters();
	}
	gui->biorulesConnection->closeSession();
	gui->biorulesConnection->disconnectFromServer();
	

	exited = true;
};
void StimThread::stop(){
	stopped = true;
	while(!exited){
		stopped = true;
		Sleep(1);
	}
};

void StimThread::sendStartSignalCOM(int frameIntMS){
		
	QString command = QString().sprintf("start %d\n",frameIntMS*10);
	WriteFile(hCom,command.toAscii().constData(),command.size(),&txBytes,NULL);

};
void StimThread::sendStopSignalCOM(){
	QString command = "stop\n";
	WriteFile(hCom,command.toAscii().constData(),command.size(),&txBytes,NULL);
};

//Build packet
void StimThread::makeResultPacket(int ind, char * buffer){
	
	AnalysisModule * aMod = gui->expAgent->analysisMod;
	double umPerPixel = gui->camThread->umPerPixel;
	double tickPerMMx = gui->stageThread->tickPerMM_X;
	double tickPerMMy = gui->stageThread->tickPerMM_Y;
	
	sprintf(buffer,"<larva-skeleton-request,1,%s,%d,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f,%f>\n",
				gui->biorulesConnection->sessionIdChar, //field 3
				aMod->sampleTime[ind],			//4
				(aMod->head[ind].x*umPerPixel/1000.0) + aMod->stagePos[ind].x/tickPerMMx,				//5	
				(aMod->head[ind].y*umPerPixel/1000.0) + aMod->stagePos[ind].y/tickPerMMy,				//6
				(aMod->neck[ind].x*umPerPixel/1000.0) + aMod->stagePos[ind].x/tickPerMMx,				//7	
				(aMod->neck[ind].y*umPerPixel/1000.0) + aMod->stagePos[ind].y/tickPerMMy,				//8
				(aMod->tail[ind].x*umPerPixel/1000.0) + aMod->stagePos[ind].x/tickPerMMx,				//9	
				(aMod->tail[ind].y*umPerPixel/1000.0) + aMod->stagePos[ind].y/tickPerMMy,				//10
				aMod->length[ind]*umPerPixel/1000.0,				//11
				(aMod->centroid[ind].x*umPerPixel/1000.0) + aMod->stagePos[ind].x/tickPerMMx,				//12	
				(aMod->centroid[ind].y*umPerPixel/1000.0) + aMod->stagePos[ind].y/tickPerMMy,				//13
				aMod->headToBodyAngle[ind]*180.0/M_PI,		//14
				//aMod->tailBearingAngle[ind]*180.0/M_PI);	//15
				aMod->newTailBearing[ind]*180.0/M_PI); //15



	//qDebug(buffer);

};

QString StimThread::makeStimPacket(QStringList & response){
	double intensity;
	int duration;
	QString command("wave ");

	for(int i=5; i<response.size(); i+=2){
		intensity = response[i].toDouble();
		duration = response[i+1].toInt();
		command += QString().sprintf("%d %d ",int(intensity * double(0xffff)/100.0),duration*10);
		//command += QString().sprintf("%03x,%02x,",int(intensity * double(0xfff)/100.0),15);
	}
	command.truncate(command.size()-1);
	command += "\n";

	return command;
};

void StimThread::queueManualStim(int pAmp, int pDuty, int nRepeat, int period){
	//generates a waveform with nrepeat periods at period width in ms, duty cycle in percent
	// amplitude is in percent of max
	if (stimQueued) return; 
	
	qAmp = pAmp;
	qStim = "wave ";
	for(int i=0; i<nRepeat; i++){
		qStim += QString().sprintf("%d %d ",(pAmp*0xffff)/100,10*(period*pDuty)/100);
		if(pDuty!=100)
			qStim += QString().sprintf("%d %d ",0,10*(period - period*pDuty/100));
	}
	qStim.truncate(qStim.size()-1);  //remove final comma
	qStim += "\n";

	stimQueued = true;

};

int StimThread::readTimeResponseCOM(){
	char rbuf[50];
	int bPtr = 0;
	int val;

	//Lakshmi's System
	//ReadFile(hCom,rbuf,4,&rxBytes,NULL);
	//val = 256*256*256*int(rbuf[0]) + 256*256*int(rbuf[1]) + 256*int(rbuf[2]) + rbuf[3];
	
	//Magnus's board
	while(1){
		ReadFile(hCom,&rbuf[bPtr],1,&rxBytes,NULL);
		if(rbuf[bPtr] == '\n'){
			rbuf[bPtr] = 0;
			break;
		}
		bPtr++;
	}
	val = atoi(rbuf);
	return val;
};

bool StimThread::connectCOM(){
	//Connect to serial port for stage
	hCom = CreateFile(comName.utf16(),GENERIC_READ|GENERIC_WRITE,0,0,OPEN_EXISTING,0,0); //Zaber Stage Control
	if (hCom == INVALID_HANDLE_VALUE) {
		qDebug("Can't Connect to Stim");
		return false;
	}
	//Configure Ports
	DCB cfg;
	GetCommState(hCom,&cfg);
	//cfg.BaudRate = 1200000;
	//cfg.BaudRate = 115200; //for magnus' board
	cfg.BaudRate = 460800; //Lakshmi's system
	//cfg.BaudRate = 230400; //Lakshmi's system
	cfg.ByteSize = 8;
	cfg.StopBits = ONESTOPBIT;
	cfg.Parity = NOPARITY;
	cfg.fDtrControl = DTR_CONTROL_DISABLE;
	cfg.fRtsControl = RTS_CONTROL_DISABLE;
	if(!SetCommState(hCom, &cfg )){
		qDebug("Serial SetCommState Error %d - Stim ",GetLastError());
		return false;
	}
	
	COMMTIMEOUTS timeouts;
	timeouts.ReadIntervalTimeout = 0;
	timeouts.ReadTotalTimeoutMultiplier = 100;
	timeouts.ReadTotalTimeoutConstant = 100;
	timeouts.WriteTotalTimeoutMultiplier = 100;
	timeouts.WriteTotalTimeoutConstant = 100;
	if (!SetCommTimeouts(hCom,&timeouts)) {
		qDebug("SetCommTimeouts Error 1 - Stim");
		return false;
	}

	if (!SetupComm(hCom,2048,2048)){
		qDebug("SetupComm Error 1 - Stim"); 
		return false;
	}

	return true;
};
bool StimThread::disconnectCOM(){
	return CloseHandle(hCom);
};

