/*
	Main Larva Tracking Computer Vision Algorithm

*/
#include "analysisModule.h"
#include "stageThread.h"
#include <opencv2/opencv.hpp>

//profiling functions
void AnalysisModule::tic(){
	QueryPerformanceFrequency(&pcFreq);
	QueryPerformanceCounter(&pcTic);
};
double AnalysisModule::toctic(){
	QueryPerformanceCounter(&pcToc);
	double diff = double(pcToc.QuadPart - pcTic.QuadPart)/double(pcFreq.QuadPart);
	QueryPerformanceCounter(&pcTic);
	return diff;
};

//Main tracking Algorithm
void AnalysisModule::larvaFind(uchar * img, int imWidth, int imHeight, int frameInd){

	input = cv::Mat(imHeight,imWidth,CV_8UC1,NULL);
	input.data = img;
	if(output.rows != imHeight | output.cols != imWidth) output.create(imHeight,imWidth,CV_8UC1);
	int nextInd = (index+1)%sampleInd.size();

	//for Profiling
	tic();
		
	sampleInd[nextInd] = frameInd;
	sampleTime[nextInd] = frameInd * frameIntervalMS;

	//On first image, automatically determine threshold level using the Otsu method
	// Minimizes within group variance of thresholded classes.  Should land on the best boundary between backlight and larva
	if(index == -1) threshold = otsuThreshold(img,imWidth*imHeight);
	

	//Can speed this up by applying to a roi bounding box a bit larger than the previous one

	//Simple inverted binary threshold of the image
	cv::threshold(input,output,threshold,255,CV_THRESH_BINARY_INV);  profile[0] = toctic();
	//Detect Contours in the binary image
	cv::findContours(output,contours,CV_RETR_EXTERNAL,CV_CHAIN_APPROX_NONE);  profile[1] = toctic();
	
	//No contours detected
	if (contours.size() == 0) {
		return;
	}

	//find contour with largest perimeter length
	double maxLen = 0; int maxInd = -1;
	double cLen;
	for(int i=0; i<contours.size(); i++){
		cLen = cv::arcLength(cv::Mat(contours[i]), false);
		if(cLen >= maxLen){ maxLen = cLen; maxInd = i; };
	}
	
	//Check to make sure that the perimeter is a larva by simple size analysis 
	//(larva should have a certain perimeter length at 8.1um/pixel)
	cLarva[nextInd] = contours[maxInd];
	
	//calculate bounding box
	bBox[nextInd] = cv::boundingRect(cv::Mat(cLarva[nextInd])); profile[2] = toctic();
	
	//Calculate fourier coefficients
	fourierDecompose(cLarva[nextInd],nFourier,fourier[nextInd]);
	centroid[nextInd] = cv::Point2f(fourier[nextInd][0][AX],fourier[nextInd][0][AY]); profile[3] = toctic();

	//Reconstruct the estimated boundary
	fourierReconstruct(fourier[nextInd],cFit,fitRes); profile[4] = toctic();
	
	//Calculate Curvature
	perimeterCurvature(cFit,curve,fitRes/8); profile[5] = toctic();

	//Find head and tail based on curvature minimums (small angle = sharp region)
	findHeadTail(cFit,curve,headTail); 
	head[nextInd] = headTail[0];
	tail[nextInd] = headTail[1]; profile[6] = toctic();

	//Calculate Skeleton
	skeletonCalc(cFit,skeleton,headTail,length[nextInd],neck[nextInd]); profile[7] = toctic();
	

	//Calculate bearing and head angle to bearing
	bodyAngles(tailBearingAngle[nextInd], headToBodyAngle[nextInd], head[nextInd], neck[nextInd], tail[nextInd]); profile[8] = toctic();
	

	//Capture stage position 
	stagePos[nextInd] = cv::Point(gui->stageThread->xpos,gui->stageThread->ypos);
	
	//Keep track of entire history with a sample every 30 frames
	if((nextInd % 30) == 0){
		fullTrack[(fullTrackInd+1)%fullTrack.size()].x = stagePos[nextInd].x/gui->stageThread->tickPerMM_X+centroid[nextInd].x*gui->camThread->umPerPixel/1000.0;
		fullTrack[(fullTrackInd+1)%fullTrack.size()].y = stagePos[nextInd].y/gui->stageThread->tickPerMM_Y+centroid[nextInd].y*gui->camThread->umPerPixel/1000.0;
		fullTrackStim[(fullTrackInd+1)%fullTrack.size()] = binStimMax;
		binStimMax = 0; //updated from stimThread
		fullTrackInd++;
	}
	

	//Calculate Velocities of head and tail
	calcVelocities(nextInd);

	//Spew out profiling info
	//for(int i=0; i<9; i++) qDebug("%d: %.4fms",i,profile[i]*1000);
	//qDebug("\n");
	
	index++;

};

int AnalysisModule::otsuThreshold(uchar *img, int imPixelCount){
	//Automatically determine a threshold using the histogram
	// Otsu Method = Threshold value that maximizes within group variance
	//
	// Algorithm derived from description in Shapiro & Stockman, "Computer Vision," 2001
	
	//Calculate image histogram
	std::vector<double> hist;
	hist.resize(256);
	for(int i=0; i<256; i++) hist[i] = 0;  //initialize histogram
	for(int i=0; i<imPixelCount; i++) hist[img[i]]++;  //count pixels
	for(int i=0; i<256; i++) hist[i] /= double(imPixelCount);  //normalize

	std::vector<double> sigW; sigW.resize(256);
	double q1,q2,u1,u2,s1,s2;

	//for all possible threshold values, find value that minimizes within group variance
	for(int i=0; i<256; i++){

		q1 = 0; q2 = 0; u1 = 0; u2 = 0; s1 = 0; s2 = 0;
		for(int j=0; j<i; j++)	q1 += hist[j]; //normalization factors for the group
		for(int j=i; j<256; j++) q2 += hist[j];
		if (q1 == 0 | q2 == 0) {sigW[i] = -1; continue;}  //bad value, members all in one group

		for(double j=0; j<i; j++) u1 += (j*hist[j]/q1); //within group mean
		for(double j=i; j<256; j++) u2 += (j*hist[j]/q2);

		for(double j=0; j<i; j++) s1 += pow((j-u1),2)*hist[j]/q1;  //within group variance
		for(double j=i; j<256; j++) s2 += pow((j-u2),2)*hist[j]/q2;

		sigW[i] = q1*s1 + q2*s2;  //Weighted sum of within group variance
	}

	//Find minimum valid value of weighted sum of within group variances
	int minInd;
	for(int i=0; i<256; i++)
		if (sigW[i] != -1){minInd = i; break;}
		
	for(int i=1; i<255; i++) if( (sigW[i] < sigW[minInd]) & (sigW[i] != -1)) minInd = i;

	return minInd;  //return the index with minimum within group variance

};

//Carry out the fourier decomposition of a contour
void AnalysisModule::fourierDecompose(std::vector<cv::Point> contour, int nComponents, std::vector<std::vector<double>> & coeffs){

	int cSize = contour.size();
	
	if(coeffs.size()!=nComponents) coeffs.resize(nComponents);
	//Fourier fit w/ nComponents descriptors.
	for (int i=0; i<nComponents; i++){
		//Initialize
		if(coeffs[i].size() != 4) coeffs[i].resize(4);
		//0:AX,1:BX,2:AY,3:BY - defined in enum fourCoef
		for(int j=0; j<4; j++) coeffs[i][j] = 0.0;

		//Dot Products
		for (int j=0; j<cSize; j++){
			coeffs[i][AX] += double(contour[j].x) * cos(double(i) * M_PI * double(j) * 2.0 / double(cSize));
			coeffs[i][BX] += double(contour[j].x) * sin(double(i) * M_PI * double(j) * 2.0 / double(cSize));
			coeffs[i][AY] += double(contour[j].y) * cos(double(i) * M_PI * double(j) * 2.0 / double(cSize));
			coeffs[i][BY] += double(contour[j].y) * sin(double(i) * M_PI * double(j) * 2.0 / double(cSize));
		}
		//Normalize
		for (int j=0; j<4; j++) coeffs[i][j] *= 2.0/double(cSize);
	}
	coeffs[0][AX] /= 2.0;
	coeffs[0][AY] /= 2.0;

};

//Reconstruct a low pass filtered version of the contour with n points of resolution
void AnalysisModule::fourierReconstruct(std::vector<std::vector<double>> coeffs, std::vector<cv::Point2f> & cFit, int nPoints){

	cFit.resize(nPoints);

	//theta goes from -PI to PI in nPoints steps
	double theta = -M_PI;
	double thetaStep = 2.0*M_PI/double(nPoints);

	for(int i=0; i<cFit.size(); i++) cFit[i] = cv::Point2f(0,0);  //initialize points

	for(int i=0; i<coeffs.size(); i++){
		theta = -M_PI;
		for(int j=0; j<cFit.size(); j++){
			cFit[j].x += coeffs[i][AX] * cos(double(i) * theta) + coeffs[i][BX]*sin(double(i) * theta);
			cFit[j].y += coeffs[i][AY] * cos(double(i) * theta) + coeffs[i][BY]*sin(double(i) * theta);
			theta += thetaStep;
		}
	}

};

void AnalysisModule::perimeterCurvature(std::vector<cv::Point2f> & cFit, std::vector<double> & curve, int curveDist){

	//calculate curvature (with wrapping) - Based on modified algorithm provided by Alex Gomez
	curve.resize(fitRes);

	for (int i=0; i<fitRes; i++){
		if (i<curveDist) 
			curve[i] = atan2( cFit[i+curveDist].y-cFit[i].y , cFit[i+curveDist].x-cFit[i].x ) 
						- atan2( cFit[fitRes-(curveDist-i)].y-cFit[i].y , cFit[fitRes-(curveDist-i)].x-cFit[i].x );
		else if (i>(fitRes-curveDist-1))
			curve[i] = atan2(cFit[curveDist-(fitRes-i)].y-cFit[i].y,cFit[curveDist-(fitRes-i)].x-cFit[i].x) 
						- atan2(cFit[i-curveDist].y-cFit[i].y,cFit[i-curveDist].x-cFit[i].x);
		else 
			curve[i] = atan2(cFit[i+curveDist].y-cFit[i].y,cFit[i+curveDist].x-cFit[i].x) 
						- atan2(cFit[i-curveDist].y-cFit[i].y,cFit[i-curveDist].x-cFit[i].x);

		if (curve[i]<0) curve[i] = curve[i] + 2*M_PI;
	}


};

//Given a perimeter curvature, find head and tail
void AnalysisModule::findHeadTail(std::vector<cv::Point2f> & cFit, std::vector<double> crv, std::vector<cv::Point2f> & headTail){

	//Smallest (sharpest internal angle) curvature point is head (typically), suppress values around this point.  
	//Once one end is clearly much larger in curvature, track that one as the head using proximity measures.
	//Next minimum curvature point is tail, bends in body have positive curvature, so don't get picked up

	//find first minimum of curvature (sharpest point is typically the head)
	int minInd=0; double minVal=crv[0];
	for(int i=0; i<crv.size(); i++)
		if(crv[i] < minVal){ minVal = crv[i]; minInd = i;}
	
	headTail.resize(2);
	headTail[0] = cFit[minInd];

	//ignore values around this min and find the next minimum (typically tail)
	//this loop does not modify the variable curve handed in since it passes by copy
	int suppress = fitRes/8;  //suppress 25% of the perimeter	
	for (int i= minInd-suppress; i<(minInd+suppress); i++){
		if (i<0)				crv[fitRes+i] = 600;
		if (i>=fitRes)			crv[i-fitRes] = 600;
		if((i>=0) & (i<fitRes))	crv[i] = 600;
	}

	//find next min
	minInd=0; minVal=crv[0];
	for(int i=0; i<crv.size(); i++)
		if(crv[i] < minVal){ minVal = crv[i]; minInd = i;}

	headTail[1] = cFit[minInd];

	//Determine which is head and which is tail based on long term curvature min and forward walking
	if(index == -1){
		masterHeadTail = headTail;
		votesHT[0]++;
	}else{
		//Determine proximities to detect if a flip occurred
		distHT[0] = pow(masterHeadTail[0].x - headTail[0].x,2) + pow(masterHeadTail[0].y - headTail[0].y,2);
		distHT[1] = pow(masterHeadTail[0].x - headTail[1].x,2) + pow(masterHeadTail[0].y - headTail[1].y,2);
		distHT[2] = pow(masterHeadTail[1].x - headTail[0].x,2) + pow(masterHeadTail[1].y - headTail[0].y,2);
		distHT[3] = pow(masterHeadTail[1].x - headTail[1].x,2) + pow(masterHeadTail[1].y - headTail[1].y,2);
		
		minInd = 0; //find minimum
		for(int i=0; i<4; i++) if(distHT[i] < distHT[minInd]) minInd = i;

		//Update the coordinates of the proximity tracked points
		if(minInd == 0 | minInd == 3){//No flip has occurred
			masterHeadTail = headTail;
			votesHT[0]++;
		}else{
			//flip them into the initial head tail, give a vote to second entry that was not initially sharpest
			masterHeadTail[0] = headTail[1];
			masterHeadTail[1] = headTail[0];
			votesHT[1]++;
		}

		//Also add votes to the point in the direction of motion when the head/tail angle is small
		//(assumes the animal goes mostly forward when walking)
		if(index>-1){ //need history to calculate a velocity direction
			//Currently not implemented
		}		
	}

	//Assign based on votes
	//The master point with the highest number of votes is placed first in the return list to represent the head
	if(votesHT[0]>= votesHT[1]){
		headTail = masterHeadTail;
	}else{
		headTail[0] = masterHeadTail[1];
		headTail[1] = masterHeadTail[0];
	}


};

//Calculate the skeleton (midline) of the perimeter based on head and tail locations
void AnalysisModule::skeletonCalc(std::vector<cv::Point2f> & cFit, std::vector<cv::Point2f> & skeleton, std::vector<cv::Point2f> & headTail, double & skelLen, cv::Point2f & neck){

	//Determine the head index in the perimeter
	int headInd;
	for(int i=0; i<cFit.size(); i++)
		if(headTail[0].x == cFit[i].x & headTail[0].y == cFit[i].y) {headInd = i; break;}

	int tailIndCW, tailIndCCW;
	double pLengthCW = 0;
	double pLengthCCW = 0;
	//Sort perimeter from head clockwise, find number of points to tail and length of this side
	for (int i = headInd; i<(headInd+cFit.size()); i++){
		cFitSortedCW[i-headInd] = cFit[i%cFit.size()];
		//stop once we get tot he tail
		if(cFit[i%cFit.size()].x == headTail[1].x & cFit[i%cFit.size()].y == headTail[1].y){
			tailIndCW = i-headInd;
			break;
		}
	}
	//Sort perimeter from head counterclockwise, Find number of points to tail
	for(int i = headInd+cFit.size(); i>headInd; i--){
		cFitSortedCCW[(headInd+cFit.size()) - i] = cFit[i%cFit.size()];
		if(cFit[i%cFit.size()].x == headTail[1].x & cFit[i%cFit.size()].y == headTail[1].y){
			tailIndCCW = headInd+cFit.size() - i;
			break;
		}
	}


	//100 points along the skeleton, cut the different sides into a number of equal steps
	double stepCW = double(tailIndCW)/double(skeleton.size());
	double stepCCW = double(tailIndCCW)/double(skeleton.size());
	double indCW = 0;
	double indCCW = 0;

	//calculate skeleton by finding the midpoint between evenly spaced number of points on each side
	for(int i=0; i<skeleton.size(); i++){

		skeleton[i].x = (cFitSortedCW[int(indCW)].x + cFitSortedCCW[int(indCCW)].x)/2.0;
		skeleton[i].y = (cFitSortedCW[int(indCW)].y + cFitSortedCCW[int(indCCW)].y)/2.0;

		indCW += stepCW;
		indCCW += stepCCW;

	}

	//calculate skeleton length
	skelLen = 0;
	for(int i=1; i<skeleton.size(); i++)
		skelLen += sqrt( pow(skeleton[i-1].x - skeleton[i].x ,2) + pow( skeleton[i-1].y - skeleton[i].y ,2) );

	//Find neck
	double runLen = 0;
	for(int i=1; i<skeleton.size(); i++){
		runLen += sqrt( pow(skeleton[i-1].x - skeleton[i].x ,2) + pow( skeleton[i-1].y - skeleton[i].y ,2) );
		if(runLen > (neckPercentage * skelLen)){
			neck = skeleton[i];
			break;
		}
	}

	//Find angles from fits to skeleton
	//Skeleton is 100 points long, take first 5 and last five.
	//Skeleton[0] is head, skeleton[99] is tail
	
	//Regress tail bearing line
	//cv::Mat A = cv::Mat::zeros(2,2,CV_64FC1);
	//cv::Mat Ainv = A;
	//cv::Mat C = cv::Mat::zeros(2,1,CV_64FC1);
	//cv::Mat output = C;

	//double xSum = 0;
	//double xMean = 0;

	int fitDist = 75;
	int fitStart = skeleton.size()-25;
	//for(int i=0; i<fitDist; i++){
	//	A.at<double>(0,0) += skeleton[fitStart-i].x*skeleton[fitStart-i].x;
	//	A.at<double>(0,1) += skeleton[fitStart-i].x;
	//	A.at<double>(1,0) += skeleton[fitStart-i].x;
	//	A.at<double>(1,1) += 1;

	//	C.at<double>(0,0) += skeleton[fitStart-i].x * skeleton[fitStart-i].y;
	//	C.at<double>(1,0) += skeleton[fitStart-i].y;

	//	xMean += skeleton[fitStart-i].x;
	//}
	//
	////qDebug("%f",cv::determinant(A));
	//cv::invert(A,Ainv);

	//output.at<double>(0,0) = Ainv.at<double>(0,0)*C.at<double>(0,0) +
	//						 Ainv.at<double>(0,1)*C.at<double>(1,0);
	//output.at<double>(1,0) = Ainv.at<double>(1,0)*C.at<double>(0,0) +
	//						 Ainv.at<double>(1,1)*C.at<double>(1,0);

	//double m = output.at<double>(0,0);
	//double b = output.at<double>(1,0);

	//double tailBearing = double(atan2(double((m*skeleton[fitStart-fitDist].x+b) - (m*skeleton[fitStart].x+b)),
	//							double(skeleton[fitStart-fitDist].x-skeleton[fitStart].x)));

	//Simple two point based line
	double tailBearing;
	tailBearing = atan2(double(skeleton[fitStart].y - skeleton[fitStart-fitDist].y),
								double(skeleton[fitStart].x - skeleton[fitStart-fitDist].x));


	//create a derivative of tail bearing and apply a smoothing filter
	int ind = (index+1);
	newTailBearing[ind % nBuffers] = tailBearing;

	double yval;
	double accumulator=0;
	double normFactor = 0;
	double filterHistory = 30;
	double dt = double(frameIntervalMS)/1000.0; //time interval into history in seconds
	
	bool corrected;

	if (ind<=filterHistory){
		//wait for a full frame of data to filter		
	}else{
		for(int i=1; i<=filterHistory; i++) normFactor+=i;
		//ramp smoothing filter into the past
		for(int k=0; k<filterHistory; k++){
			
			
			yval = newTailBearing[(ind-k) % nBuffers];
			corrected = false;
	
			if((newTailBearing[(ind-k) % nBuffers]*180/M_PI) > 90){
				if((newTailBearing[(ind-(k+1)) % nBuffers]*180/M_PI) < -90){
					yval -= newTailBearing[(ind-(k+1)) % nBuffers] + 2*M_PI;
					corrected = true;
				}
			}

			if((newTailBearing[(ind-k) % nBuffers]*180/M_PI) < -90){
				if((newTailBearing[(ind-(k+1)) % nBuffers]*180/M_PI) > 90){
					yval -= newTailBearing[(ind-(k+1)) % nBuffers] - 2*M_PI;
					corrected = true;
				}
			}
			
			if(!corrected)
				yval -= newTailBearing[(ind-(k+1)) % nBuffers];
			
			yval /= dt;
			yval *= (filterHistory-k)/normFactor;
			accumulator += yval;
		}
	}
	dNewTailBearing[ind%nBuffers] = accumulator/2;
	
	
};

void AnalysisModule::skeletonCalcDist(){
	return;
	//ROI
	cv::Size roiSize;
	cv::Point roiOfs;
	output.locateROI(roiSize,roiOfs);
	int nextInd = (index+1) % nBuffers;
	//qDebug("%d, %d, %d, %d",roiOfs.x,roiOfs.y,roiSize.width,roiSize.height);
	
	cv::Point pts[2000];
	for(int i=0; i<cLarva[nextInd].size(); i++) pts[i] = cLarva[nextInd][i];
	cv::fillConvexPoly(output,pts,cLarva[nextInd].size(),cv::Scalar(255));
	cv::distanceTransform(output,dist,CV_DIST_L2,CV_DIST_MASK_3);
	double maxVal = 0;
	for(int i=0; i<output.rows*output.cols; i++) if(maxVal<((float*)dist.data)[i]) maxVal = ((float*)dist.data)[i];
	for(int i=0; i<output.rows*output.cols; i++) input.data[i] = uchar(255.0*((float*)dist.data)[i]/maxVal);

	/*std::string fname = "C:\\Data\\dist.png";
	cv::imwrite(fname,dist);
	fname = "C:\\Data\\output.png";
	cv::imwrite(fname,output);*/
};

//calculate body angles
void AnalysisModule::bodyAngles(double & tailBearing, double & head2BodyAngle, cv::Point2f head,cv::Point2f neck, cv::Point2f tail){

	tailBearing = atan2(tail.y-neck.y, tail.x-neck.x);
	head2BodyAngle = atan2((head.y-neck.y)*cos(tailBearing) - (head.x-neck.x)*sin(tailBearing),
						   (head.y-neck.y)*sin(tailBearing) + (head.x-neck.x)*cos(tailBearing));
		
	if (head2BodyAngle<0) head2BodyAngle = -( M_PI + head2BodyAngle );
	else head2BodyAngle = ( M_PI - head2BodyAngle );

};

//Calculate velocities for head and tail
void AnalysisModule::calcVelocities(int nextInd){

	double dt;
	cv::Point2f world[2];

	//head
	dt = double(headVelStep*frameIntervalMS)/1000.0; //time interval into history in seconds
	if(headVelStep>nextInd){
		headVelocity[nextInd] = cv::Point2f(0,0);
		headSpeed[nextInd] = 0;
	}else{
		//convert pixel values in image into world coordinates using stage location and calibration values
		//do this for position now and position some step into the past
		world[0].x = head[nextInd].x * (gui->camThread->umPerPixel/1000.0) //convert pixels into mm in camera
							+ double(stagePos[nextInd].x)/gui->stageThread->tickPerMM_X; //shift by stage position in mm
		world[0].y = head[nextInd].y * (gui->camThread->umPerPixel/1000.0) //convert pixels into mm in camera
							+ double(stagePos[nextInd].y)/gui->stageThread->tickPerMM_Y; //shift by stage position in mm
		world[1].x = head[nextInd-headVelStep].x * (gui->camThread->umPerPixel/1000.0) //convert pixels into mm in camera
							+ double(stagePos[nextInd-headVelStep].x)/gui->stageThread->tickPerMM_X; //shift by stage position in mm
		world[1].y = head[nextInd-headVelStep].y * (gui->camThread->umPerPixel/1000.0) //convert pixels into mm in camera
							+ double(stagePos[nextInd-headVelStep].y)/gui->stageThread->tickPerMM_Y; //shift by stage position in mm

		headVelocity[nextInd].x = (world[0].x - world[1].x)/dt; //x velocity mm/s
		headVelocity[nextInd].y = (world[0].y - world[1].y)/dt; //y velocity mm/s
		headSpeed[nextInd] = sqrt(pow(headVelocity[nextInd].x,2)+pow(headVelocity[nextInd].y,2));
	}

	//tail
	dt = double(tailVelStep*frameIntervalMS)/1000.0; //time interval into history
	if(tailVelStep>nextInd){
		tailVelocity[nextInd] = cv::Point2f(0,0);
		tailSpeed[nextInd] = 0;
	}else{

		world[0].x = tail[nextInd].x * (gui->camThread->umPerPixel/1000.0) //convert pixels into mm in camera
							+ double(stagePos[nextInd].x)/gui->stageThread->tickPerMM_X; //shift by stage position in mm
		world[0].y = tail[nextInd].y * (gui->camThread->umPerPixel/1000.0) //convert pixels into mm in camera
							+ double(stagePos[nextInd].y)/gui->stageThread->tickPerMM_Y; //shift by stage position in mm
		world[1].x = tail[nextInd-headVelStep].x * (gui->camThread->umPerPixel/1000.0) //convert pixels into mm in camera
							+ double(stagePos[nextInd-headVelStep].x)/gui->stageThread->tickPerMM_X; //shift by stage position in mm
		world[1].y = tail[nextInd-headVelStep].y * (gui->camThread->umPerPixel/1000.0) //convert pixels into mm in camera
							+ double(stagePos[nextInd-headVelStep].y)/gui->stageThread->tickPerMM_Y; //shift by stage position in mm

		tailVelocity[nextInd].x = (world[0].x - world[1].x)/dt; //x velocity mm/s
		tailVelocity[nextInd].y = (world[0].y - world[1].y)/dt; //y velocity mm/s
		tailSpeed[nextInd] = sqrt(pow(tailVelocity[nextInd].x,2)+pow(tailVelocity[nextInd].y,2));
	}

};