#include "analysisModule.h"

AnalysisModule::AnalysisModule(Wormsign * ui):QObject(){

	nBuffers = 2000; //if this gets changed, change it in stimthread constructor for stimAmps also
	gui = ui;
	index = -1;

	//Analysis Algorithm Parameters & Local Variables - should be read from XML file
	nFourier = 7;
	//threshold = 20;  //will be auto-detected using otsu method on first frame
	fitRes = 200;
	neckPercentage = .5; 
	tailVelStep = 1;
	headVelStep = 1;

	fullTrackInd = -1; binStimMax = 0;
	fullTrack.resize(60*60*10); //10 hours at 1s interval, should be more than enough (36000 elements)
	fullTrackStim.resize(60*60*10); //track stimulus delivered in 1s bins

	//Allocate buffer for results history 
	cLarva.resize(nBuffers);
	sampleInd.resize(nBuffers);
	sampleTime.resize(nBuffers);
	head.resize(nBuffers);
	headVelocity.resize(nBuffers);
	headSpeed.resize(nBuffers);
	neck.resize(nBuffers);
	tail.resize(nBuffers);
	tailVelocity.resize(nBuffers);
	tailSpeed.resize(nBuffers);
	length.resize(nBuffers);
	centroid.resize(nBuffers);
	headToBodyAngle.resize(nBuffers);
	tailBearingAngle.resize(nBuffers);
	bBox.resize(nBuffers);
	fourier.resize(nBuffers);
	for(int i=0; i<nBuffers; i++){
		fourier[i].resize(nFourier);
		for(int j=0; j<nFourier; j++)
			fourier[i][j].resize(4);
	}

	newTailBearing.resize(nBuffers);
	newHeadBearing.resize(nBuffers);
	dNewTailBearing.resize(nBuffers);
	
	//From serial feedback
	loopTime.resize(nBuffers);
	stimCode.resize(nBuffers);
	behaviorState.resize(nBuffers);
	stimAmps.resize(nBuffers);

	//from stage feedback
	stagePos.resize(nBuffers);

	//Profiling
	analysisTime.resize(nBuffers);
	networkTime.resize(nBuffers);

	votesHT.resize(2); votesHT[0] = 0; votesHT[1] = 0;
	distHT.resize(4);
	masterHeadTail.resize(2);
	cFit.resize(fitRes);
	cFitSortedCW.resize(fitRes);
	cFitSortedCCW.resize(fitRes);
	skeleton.resize(500);
	profile.resize(9);

};

AnalysisModule::~AnalysisModule(){

};

//returns a legend labeling the columns in the output data
QString AnalysisModule::makeDataLegend(){

	QString legend;
	legend += "1: Sample Index\n";
	legend += "2: Sample time (seconds)\n";
	legend += "3: Head x (pixels)\n";
	legend += "4: Head y (pixels)\n";
	legend += "5: Neck x (pixels)\n";
	legend += "6: Neck y (pixels)\n";
	legend += "7: Tail x (pixels)\n";
	legend += "8: Tail y (pixels)\n";
	legend += "9: Skeleton Length (pixels)\n";
	legend += "10: Centroid x (pixels)\n";
	legend += "11: Centroid y (pixels)\n";
	legend += "12: Head to Body Angle (degrees)\n";
	legend += "13: Tail Bearing Angle (degrees)\n";
	legend += "14: Stage Position x (Zaber Units)\n";
	legend += "15: Stage Position y (Zaber Units)\n";
	legend += "16: Behavior State Code\n";
	legend += "17: Loop Time (ms)\n";
	legend += "18: Stimulus Command\n";
	legend += "19+: 4*N Fourier Coefficients (ax[0],bx[0],ay[0],by[0],ax[1]...)\n";

	return legend;

}; 
//Returns a single string containing a comma delimited data sample (see legend) with a \n terminator
QString AnalysisModule::makeDataLine(int ind){

	QString aLine;
	if(ind > nBuffers | ind < 0) return aLine;

	aLine += QString().sprintf("%d, ",sampleInd[ind]);							//1
	aLine += QString().sprintf("%.3f, ",double(sampleTime[ind])/1000.0);		//2
	aLine += QString().sprintf("%.2f, ",head[ind].x);							//3
	aLine += QString().sprintf("%.2f, ",head[ind].y);							//4
	aLine += QString().sprintf("%.2f, ",neck[ind].x);							//5
	aLine += QString().sprintf("%.2f, ",neck[ind].y);							//6
	aLine += QString().sprintf("%.2f, ",tail[ind].x);							//7
	aLine += QString().sprintf("%.2f, ",tail[ind].y);							//8
	aLine += QString().sprintf("%.2f, ",length[ind]);
	aLine += QString().sprintf("%.2f, ",centroid[ind].x);
	aLine += QString().sprintf("%.2f, ",centroid[ind].y);
	aLine += QString().sprintf("%.2f, ",headToBodyAngle[ind]*180/M_PI);
	aLine += QString().sprintf("%.2f, ",tailBearingAngle[ind]*180/M_PI);
	aLine += QString().sprintf("%d, ",stagePos[ind].x);
	aLine += QString().sprintf("%d, ",stagePos[ind].y);
	aLine += QString().sprintf("%d, ",behaviorState[ind]);
	aLine += QString().sprintf("%.1f, ",loopTime[ind]);
	QString stimCommand = stimCode[ind];
	stimCommand.truncate(stimCommand.length()-1);
	aLine += stimCommand + ", ";
	for(int i=0; i<fourier[ind].size(); i++)
		for(int j=0; j<fourier[ind][i].size(); j++)
			aLine += QString().sprintf("%.4f, ",fourier[ind][i][j]);
	aLine.truncate(aLine.length()-2);
	aLine += "\n";


	return aLine;


};