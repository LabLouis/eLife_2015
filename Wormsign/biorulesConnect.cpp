#include "biorulesConnect.h"

BiorulesConnect::BiorulesConnect():QObject(){

	ip = "127.0.0.1";
	port = 43721;
	socket = NULL;
	
	bioRulesVersion = "1.0.0";
	messageVersion = "1";

	inDataSize = int(50*pow(2.0,20.0)); //50MBytes
	inData = new char[inDataSize]; //2^20 bytes (1MByte)

};
BiorulesConnect::~BiorulesConnect(){
	delete [] inData;
};

bool BiorulesConnect::connectToServer(){

	if(socket) delete socket;

	socket = new QTcpSocket();
	socket->connectToHost(ip,port);

	qDebug("Waiting for Connection to %s:%d",ip.toAscii().constData(),port);
	if(!socket->waitForConnected(10000)){
		delete socket;
		socket = NULL;
		qDebug("Connection Failed");
		return false;
	};
	
	return true;

};

void BiorulesConnect::disconnectFromServer(){
	if (!socket) return;

	socket->disconnectFromHost();
	delete socket;
	socket = NULL;

};

QStringList BiorulesConnect::listConfigs(){
	
	if(!socket) return QStringList();

	//parse string
	QStringList configs;
	//configs << QString("NONE");
	
	//list configurations request command
	QString out = "<list-configurations-request," + messageVersion + ">\n";
	socket->write(out.toAscii().constData());

	//wait for biorules to respond
	if(!socket->waitForReadyRead(10000)){
		qDebug("NO RESPONSE to list-configuration-request");
		return configs;
	};

	//Read response
	socket->readLine(inData,2048);

	//Extract Fields
	QStringList fields = getFields(inData);

	//Field 0,1,2 are type, version, status code respectively
	for(int i=3; i<fields.size(); i++) configs << fields[i];
	
	//Should check head, version, and status for errors

	//store the list in the connection object
	configList = configs;
	

	LARGE_INTEGER pcTic, pcToc, pcFreq;
	double timeDiff;
	QueryPerformanceFrequency(&pcFreq);

	//request images associated with each 
	fieldImages.resize(configList.size());
	for(int i=0; i<configList.size(); i++){
		QueryPerformanceCounter(&pcTic);
		
		//request image, store in fieldImages
		getFieldImage(configList[i],fieldImages[i]);

		QueryPerformanceCounter(&pcToc);
		timeDiff = double(pcToc.QuadPart - pcTic.QuadPart)/double(pcFreq.QuadPart);
		qDebug("TOTAL GETFIELDIMAGE Time (%d/%d): %.1f ms",i+1,configList.size(),timeDiff*1000.0);
	}


	return configs;

};

//function to return an image that defines the virtual gradient
//(8-bit, 3 channel (gray level stored in blue chan)
void BiorulesConnect::getFieldImage(QString configName, cv::Mat & img){
	int width=240, height=240;

	//Majority of lag on this function is in waiting for the response from the bio-rules server
	//	as eric's application reads and interpolates the field image from the matrix


	//form request packet
	QString out = "<arena-background-request," + messageVersion + "," + bioRulesVersion + "," +
		configName + QString().sprintf(",%d,%d>\n",width,height);
	//send packet, open the session temporarily
	socket->write(out.toAscii().constData());

	
	//read response
	socket->waitForReadyRead(10);
	
	int ind = socket->read(inData,inDataSize);
	while(1){
		//Could get stuck in this loop if biorule never responds with \n
		socket->waitForReadyRead(10);
		ind += socket->read(&inData[ind],inDataSize-ind);
		if(inData[ind-1] == '\n') break;
	}
	inData[ind] = 0;
	
	//Parse response (getFields is too slow as it reallocates the stringlist)
	
	int preInd;
	ind = 0;
	while(inData[ind]!=',') ind++; //end of "arena-background-response"
	ind++; while(inData[ind]!=',') ind++; //end of version "1"
	ind++; preInd = ind;  
	while(inData[ind]!=',') ind++; //end of Status code 
	inData[ind] = 0;
	int status = QString(&inData[preInd]).toInt();
	ind++; preInd = ind;
	while(inData[ind]!=',') ind++; //end of width 
	inData[ind] = 0;
	width = QString(&inData[preInd]).toInt();
	ind++; preInd = ind;
	while(inData[ind]!=',' & inData[ind]!='>' ) ind++; //end of height 
	inData[ind] = 0;
	height = QString(&inData[preInd]).toInt();

	//qDebug("Status: %d, Width: %d, Height: %d",status,width,height);


	//Colormap should be defined here.  Currently it's pulled into blue channel of RGB image

	//build image, create black image, fill in blue plane with signal
	img.create(height,width,CV_8UC3);
	img.setTo(cv::Scalar(0,0,0));  //start with BGRA image, alpha blend based on intensity of stimulus
	for(int i=0; i<height*width; i++){
		ind++; preInd = ind;
		while(inData[ind]!=',' & inData[ind]!='>' ) ind++;
		inData[ind] = 0;
		img.data[i*3 + 2] = (uchar)(QString(&inData[preInd]).toDouble()*255.0/100.0);
	}

	
};

bool BiorulesConnect::openSession(QString session){
	if(!socket) return false;

	//request the selected session
	QString out = "<open-session-request," + messageVersion + "," + bioRulesVersion + "," + session + ">\n";
	socket->write(out.toAscii().constData());

	if(!socket->waitForReadyRead(10000)) return false;

	//verify readline
	socket->readLine(inData,2048);
	//extract fields
	QStringList fields = getFields(inData);

	sessionId = fields[3];
	strcpy(sessionIdChar,sessionId.toAscii().constData());

	qDebug("Biorules Session: %s",sessionIdChar);

	//not currently checking for errors

	return true;
};

bool BiorulesConnect::closeSession(){
	if(!socket) return false;

	QString out = "<close-session-request," + messageVersion + "," + sessionId + ">\n";
	socket->write(out.toAscii().constData());

	if(!socket->waitForReadyRead(10000)) return false;

	//verify readline
	socket->readLine(inData,2048);

	QStringList fields = getFields(inData);

	//Currently do nothing with response (should be "status-response" message)

	return true;

};

QStringList BiorulesConnect::readResponse(){
	QStringList response;
	if(!socket) return response;

	if(!socket->waitForReadyRead(10000)) return response;

	socket->readLine(inData,2048);
	//qDebug(inData);
	response = getFields(inData);

	return response;
};

//General purpose string parser for fields in our data format
//Currently no error checking
QStringList BiorulesConnect::getFields(char * data){

	QStringList fields;
	QString field;

	//Find commas in null terminated input array
	std::vector<int> commas;
	int dataLen = strlen(data);
	for(int i=0; i<dataLen; i++){
		if(data[i] == ',') commas.push_back(i);
		if(data[i] == '>') data[i] = 0;  //null terminate the end of the brackets
	}

	//replace commas with null terminators, but remember their locations
	//This creates many null terminated substrings within the buffer
	for(int i=0; i<commas.size(); i++) data[commas[i]] = 0;

	//get first field
	field = (data+1); //just after the '<'
	fields << field;

	//Loop through the rest of the fields
	for(int j=0; j<(commas.size()); j++){
		field = &data[commas[j]+1]; //Pointer to the address just past the comma
		fields << field;
	}

	return fields;
};

//Request the XML file to log that contains the parameters
void BiorulesConnect::getRuleParameters(){
	QString paramRequest = "<get-session-parameters-request,1," + sessionId	+ ">\n";
	socket->write(paramRequest.toAscii().constData());

	qDebug("reading parameters");
	//QByteArray response;
	socket->waitForReadyRead(20000);
	//50MB limit in response size
	//response = socket->readLine(5*int(pow(10.0,7.0)));
	//if (response.size() == 0) return;

	LARGE_INTEGER pcFreq, pcTic, pcToc;
	QueryPerformanceFrequency(&pcFreq);
	double pcDiff;
	QueryPerformanceCounter(&pcTic);


	for(int i=0; i<inDataSize; i++) inData[i] = 0;
	int ind = socket->read(inData,inDataSize);
	while(1){
		//Could get stuck in this loop if the system never returns a \n
		socket->waitForReadyRead(10);
		ind += socket->read(&inData[ind],inDataSize-ind);
		if(inData[ind-1]=='\n') break;
	}
	inData[ind-2] = 0; //remove the ">\n"

	
	QueryPerformanceCounter(&pcToc);
	pcDiff = double(pcToc.QuadPart-pcTic.QuadPart)/double(pcFreq.QuadPart);
	qDebug("Finished polling %d bytes in %.2fms",ind,pcDiff*1000.0); 
	
	//parse out the data
	std::vector<int> commas;
	for(int i=0; i<ind; i++){
		if(inData[i]==',') commas.push_back(i);
	}
	QString totalResponse(inData);
	
	QByteArray response;
	//convert last section from base64 to string, experiment agent will write it
	if(commas.size() == 3){
		response = &inData[commas[2]];
		paramResponse = QString(QByteArray::fromBase64(response));
	}
	

	
	
};