#include "stageThread.h"
#include "experimentAgent.h"
#include "analysisModule.h"

StageThread::StageThread(Wormsign * ui){
	exited = true;
	stopped = true;

	gui = ui;
	comName = QString("COM3");  //default
	
	//From Calibration
	tickPerMM_X = 2007;
	tickPerMM_Y = 2032;

	xpos = 0; ypos = 0; xposMM = 0; yposMM = 0;
	state = NONE;

	vel = 0; accel = 0;

};
StageThread::~StageThread(){stop();};

//Blocking cyclick polling to control motor and retrieve position
void StageThread::run(){
	
	index = -1;
	if(!connectCOM()) {stopped = true; return;}
	int nextInd;
	double dx, dy;

	//if values haven't been read yet, read them out
	if(vel == 0){
		vel = readVelocityTarget();
		accel = readAcceleration();
	}else{
		//otherwise, apply the values from the UI
		//initial value was 4687
		setVelocityTarget(vel);
		//initial value was 50
		setAcceleration(accel);

		vel = readVelocityTarget();
		accel = readAcceleration();

	}

	LARGE_INTEGER pcTic, pcToc, pcFreq;
	double diff;
	QueryPerformanceFrequency(&pcFreq);
	
	stopped = false;
	exited = false;

	while(!stopped){
		
		switch(state){
			case NONE:
				Sleep(10);
				break;
			case HOME:
				sendHome();
				state = NONE;
				break;
			case POLLPOSOTION:  //Simply poll stage position 
				PurgeComm(hCom,PURGE_RXCLEAR);
				pollPosition();
				emit posUpdate();
				break;

			case EXPERIMENT:  //Send movement commands and poll position
				if(!gui->expAgent) continue; //should not be the case, the expagent put it in this mode
				if(gui->expAgent->analysisMod->index == -1) continue;
				
				QueryPerformanceCounter(&pcTic);
				//request position
				pollPosition();
								
				//send move command from latest analysis frame (don't need to do it for every one)	
				nextInd = (gui->expAgent->analysisMod->index)%gui->expAgent->analysisMod->nBuffers;
				
				//Calculate difference in image in pixels
				dx = (double(gui->expAgent->analysisMod->centroid[nextInd].x) - double(gui->camThread->width)/2.0);
				dy = (double(gui->expAgent->analysisMod->centroid[nextInd].y) - double(gui->camThread->height)/2.0);
				//convert from pixels to mm 
				dx *= gui->camThread->umPerPixel/1000.0;
				dy *= gui->camThread->umPerPixel/1000.0;
				//convert from mm to motor steps
				dx *= tickPerMM_X;
				dy *= tickPerMM_Y;
					
				//Calibration doesn't seem to match, causing overshoot
				// Instead, only command the stage to go part of the way to target, this will converge to the 
				// final desired position
				dx *= .5;
				dy *= .5;
				Sleep(10);
				if(abs(dx) > 100 | abs(dy) > 100)
					sendRelativeMove(int(dx),int(dy));
				
				//fastPacket(int(dx), int(dy));
				QueryPerformanceCounter(&pcToc);
				
				//Profiled time to communicate with the stage
				diff = 1000.0*double(pcToc.QuadPart-pcTic.QuadPart)/double(pcFreq.QuadPart);

				emit posUpdate();
				break;
		}
	}

	disconnectCOM();
	
	exited = true;
};
void StageThread::stop(){
	stopped = true;
	while(!exited){
		stopped = true;
		Sleep(1);
	}
};

void StageThread::sendHome(){
	cBuf[0]=0; cBuf[1]=1; cBuf[2]=0; cBuf[3]=0; cBuf[4]=0; cBuf[5]=0;
	WriteFile(hCom,cBuf,6,&txBytes,NULL);
};

void StageThread::setVelocityTarget(int vTarget){

	//initial value was 4687
	uchar * cptr = (uchar*)&vTarget;
	ucBuf[0] = 23; ucBuf[1] = 42; ucBuf[2] = cptr[0]; ucBuf[3] = cptr[1]; ucBuf[4] = cptr[2]; ucBuf[5] = cptr[3];
	WriteFile(hCom,ucBuf,6,&txBytes,NULL);
	Sleep(25);
	//ucBuf[0] = 3;
	//WriteFile(hCom,ucBuf,6,&txBytes,NULL);
	Sleep(25);
	ucBuf[0] = 45;
	WriteFile(hCom,ucBuf,6,&txBytes,NULL);
	Sleep(25);
	//ucBuf[0] = 5;
	//WriteFile(hCom,ucBuf,6,&txBytes,NULL);
	Sleep(25);

};


int StageThread::readVelocityTarget(){
	int command = 42;
	uchar *cptr = (uchar*)&command;
	Sleep(100);
	PurgeComm(hCom,PURGE_RXCLEAR);

	ucBuf[0] = 2; ucBuf[1] = 53; ucBuf[2] = cptr[0]; ucBuf[3] = cptr[1]; ucBuf[4] = cptr[2]; ucBuf[5] = cptr[3];
	WriteFile(hCom,ucBuf,6,&txBytes,NULL);
	ReadFile(hCom,ucBuf,6,&rxBytes,NULL);
	
	//qDebug("%d,%d,%d,%d,%d,%d",ucBuf[0],ucBuf[1],ucBuf[2],ucBuf[3],ucBuf[4],ucBuf[5]);

	cptr = &ucBuf[2];
	command = *((int*)cptr);

	return command;
};

void StageThread::setAcceleration(int accel){

	//initial value was 50
	uchar * cptr = (uchar*)&accel;
	ucBuf[0] = 23; ucBuf[1] = 43; ucBuf[2] = cptr[0]; ucBuf[3] = cptr[1]; ucBuf[4] = cptr[2]; ucBuf[5] = cptr[3];
	WriteFile(hCom,ucBuf,6,&txBytes,NULL);
	Sleep(25);
	//ucBuf[0] = 3;
	//WriteFile(hCom,ucBuf,6,&txBytes,NULL);
	Sleep(25);
	ucBuf[0] = 45;
	WriteFile(hCom,ucBuf,6,&txBytes,NULL);
	Sleep(25);
	//ucBuf[0] = 5;
	//WriteFile(hCom,ucBuf,6,&txBytes,NULL);
	Sleep(25);
}

int StageThread::readAcceleration(){
	int command = 43;
	uchar *cptr = (uchar*)&command;

	Sleep(100);
	PurgeComm(hCom,PURGE_RXCLEAR);
	ucBuf[0] = 2; ucBuf[1] = 53; ucBuf[2] = cptr[0]; ucBuf[3] = cptr[1]; ucBuf[4] = cptr[2]; ucBuf[5] = cptr[3];
	WriteFile(hCom,ucBuf,6,&txBytes,NULL);
	ReadFile(hCom,ucBuf,6,&rxBytes,NULL);

	cptr = &ucBuf[2];
	command = *((int*)cptr);

	return command;
}


void StageThread::pollPosition(){

	//PurgeComm(hCom,PURGE_RXCLEAR);

	//request position, stage 4
	posReq[0] = 4; posReq[1] = 60;  
	WriteFile(hCom,posReq,6,&txBytes,NULL);
	ReadFile(hCom,ucBuf,6,&rxBytes,NULL);
	while(ucBuf[1] != 60){
		//qDebug("Bad x: %d, %d, %d, %d, %d, %d",ucBuf[0],ucBuf[1],ucBuf[2],ucBuf[3],ucBuf[4],ucBuf[5]);
		ReadFile(hCom,ucBuf,6,&rxBytes,NULL);
		if(rxBytes == 0) break;
	}

	xpos = 256*256*256*((unsigned int)ucBuf[5])+256*256*((unsigned int)ucBuf[4])+
		256*((unsigned int)ucBuf[3])+((unsigned int)ucBuf[2]);
	xposMM = double(xpos)/tickPerMM_X;
	
	//qDebug("x: %d, %d, %d, %d, %d, %d",ucBuf[0],ucBuf[1],ucBuf[2],ucBuf[3],ucBuf[4],ucBuf[5]);
	
	
	//Request position, stage 2 y-position
	posReq[0] = 2; posReq[1] = 60;  
	WriteFile(hCom,posReq,6,&txBytes,NULL);
	ReadFile(hCom,ucBuf,6,&rxBytes,NULL);
	
	while(ucBuf[1] != 60){
		//qDebug("Bad y: %d, %d, %d, %d, %d, %d",ucBuf[0],ucBuf[1],ucBuf[2],ucBuf[3],ucBuf[4],ucBuf[5]);
		ReadFile(hCom,ucBuf,6,&rxBytes,NULL);
		if(rxBytes == 0) break;
	}

	ypos = 256*256*256*((unsigned int)ucBuf[5])+256*256*((unsigned int)ucBuf[4])+
		256*((unsigned int)ucBuf[3])+((unsigned int)ucBuf[2]);
	yposMM = double(ypos)/tickPerMM_Y;

	//qDebug("y: %d, %d, %d, %d, %d, %d",ucBuf[0],ucBuf[1],ucBuf[2],ucBuf[3],ucBuf[4],ucBuf[5]);
};

void StageThread::sendRelativeMove(int dx, int dy){

	//PurgeComm(hCom,PURGE_RXCLEAR);
	int i;
	uchar * cptr;

	cptr = (uchar*)&dx;
	i=0;
	ucBuf[i+0] = 45;  ucBuf[i+1] = 21; ucBuf[i+2] = cptr[0]; ucBuf[i+3] = cptr[1]; ucBuf[i+4] = cptr[2]; ucBuf[i+5] = cptr[3];
	i=6;
	cptr = (uchar*)&dy;
	ucBuf[i+0] = 23;  ucBuf[i+1] = 21; ucBuf[i+2] = cptr[0]; ucBuf[i+3] = cptr[1]; ucBuf[i+4] = cptr[2]; ucBuf[i+5] = cptr[3];

	WriteFile(hCom,ucBuf,12,&txBytes,NULL);

};

void StageThread::fastPacket(int dx, int dy){

	bool xComplete = false;
	bool yComplete = false;
	uchar * cptr;
	int i;

	//request position, stage 4 - xpos
	ucBuf[0] = 4; ucBuf[1] = 60;  
	//request position, stage 2 - ypos
	ucBuf[6] = 2; ucBuf[7] = 60;  

	//Send dx
	cptr = (uchar*)&dx;
	i=12;
	ucBuf[i+0] = 45;  ucBuf[i+1] = 21; ucBuf[i+2] = cptr[0]; ucBuf[i+3] = cptr[1]; ucBuf[i+4] = cptr[2]; ucBuf[i+5] = cptr[3];
	//send dy
	cptr = (uchar*)&dy;
	i=18;
	ucBuf[i+0] = 23;  ucBuf[i+1] = 21; ucBuf[i+2] = cptr[0]; ucBuf[i+3] = cptr[1]; ucBuf[i+4] = cptr[2]; ucBuf[i+5] = cptr[3];

	int nTransmit = 24;
	if(abs(dx) < 200 & abs(dy) < 200) nTransmit = 12;  //Only send pos request

	WriteFile(hCom,ucBuf,nTransmit,&txBytes,NULL);
	rxBytes = 1;
	while((!xComplete & !yComplete) | rxBytes == 0){
		oneRead[1] = 0;
		ReadFile(hCom,oneRead,6,&rxBytes,NULL); //read single response
		if(oneRead[1] == 60){
			if(oneRead[0] == 4){ //xpos
				xpos = 256*256*256*((unsigned int)oneRead[5])+256*256*((unsigned int)oneRead[4])+
						256*((unsigned int)oneRead[3])+((unsigned int)oneRead[2]);
				xposMM = double(xpos)/tickPerMM_X;
				xComplete = true;
			}
			if(oneRead[0] == 2){  //ypos
				ypos = 256*256*256*((unsigned int)oneRead[5])+256*256*((unsigned int)oneRead[4])+
					256*((unsigned int)oneRead[3])+((unsigned int)oneRead[2]);
				yposMM = double(ypos)/tickPerMM_Y;
				yComplete = true;
			}
		}
	}





};


//Provide a string list of names of valid COM ports
//Will not return a port if it is connected in another application
QStringList StageThread::enumeratePorts(){
	
	QStringList outList;
	HANDLE h;
	QString tempName;

	for(int i=0; i<15; i++){
		tempName.sprintf("COM%d",i);
		//Try to open this COMi
		h = CreateFile(tempName.utf16(),GENERIC_READ|GENERIC_WRITE,0,0,OPEN_EXISTING,0,0);
		if(h!=INVALID_HANDLE_VALUE) {
			outList << tempName;
			CloseHandle(h); //release port
		}
	}

	return outList;
};

bool StageThread::connectCOM(){
	//Connect to serial port for stage
	hCom = CreateFile(comName.utf16(),GENERIC_READ|GENERIC_WRITE,0,0,OPEN_EXISTING,0,0); //Zaber Stage Control
	if (hCom == INVALID_HANDLE_VALUE) {
		qDebug("Can't Connect to Zaber on COM7");
		return false;
	}
	//Configure Ports
	DCB cfg;
	GetCommState(hCom,&cfg);
	cfg.BaudRate = CBR_9600;
	cfg.ByteSize = 8;
	cfg.StopBits = ONESTOPBIT;
	cfg.Parity = NOPARITY;
	cfg.fDtrControl = DTR_CONTROL_DISABLE;
	cfg.fRtsControl = RTS_CONTROL_DISABLE;
	if(!SetCommState(hCom, &cfg )){
		qDebug("Serial SetCommState Error %d - Stage ",GetLastError());
		return false;
	}
	
	COMMTIMEOUTS timeouts;
	timeouts.ReadIntervalTimeout = 0;
	timeouts.ReadTotalTimeoutMultiplier = 100;
	timeouts.ReadTotalTimeoutConstant = 100;
	timeouts.WriteTotalTimeoutMultiplier = 100;
	timeouts.WriteTotalTimeoutConstant = 100;
	if (!SetCommTimeouts(hCom,&timeouts)) {
		qDebug("SetCommTimeouts Error 1 - Stage");
		return false;
	}

	if (!SetupComm(hCom,2048,2048)){
		qDebug("SetupComm Error 1 - Stage"); 
		return false;
	}

	return true;
};
bool StageThread::disconnectCOM(){
	return CloseHandle(hCom);
};