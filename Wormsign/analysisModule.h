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
	ANALYSISMODULE.H - Analysis Module
		This object is contained within the experiment agent
		Analysis Algorithm is called in loop with frames in the camThread
		Carries out the analysis on incoming frames (calls "larvaFind" method)

		Maintains a 2000 frame deep circular Buffer of tracking data
		Stimthread monitors this object for a completed frame
		Monitored by logging thread for new data to log
		pumps an update to the UI to refresh plots
		UI plots pull data from this module for plotting

		AnalysisModule should be able to be included and called from any application.
		All that you need to do is hand the "larvaFind" algorithm an image and then acess the results in the
		results buffers.  It does not depend upon any components of the application to work.

		Most methods are created so that they can be called externally or internal to the object.
		For example, the camViewGL code calls the fourierReconstruct() method to rebuild the perimeter
		for drawing in the visualization window.

		The analysis methods are contained in larvaFind.cpp while the object constructor and destructor
		are contained in analysisModule.cpp. 
*/

#ifndef ANALYSISMODULE_H
#define ANALYSISMODULE_H

#include <QtCore>
#include <opencv2/opencv.hpp>
#include "wormsign.h"

class AnalysisModule : public QObject
{
	Q_OBJECT

public:
	AnalysisModule(Wormsign * ui);
	~AnalysisModule();

	Wormsign * gui;

	QString makeDataLegend(); //returns a legend labeling the columns in the output data
	QString makeDataLine(int ind);  //returns a line of data from the buffer

	int index;  //master index pointing to the latest analyzed frame
	int frameIntervalMS;

	int nBuffers;  //depth of buffer in frames
	enum BehaviorState {NONE,RUN,TLEFT,TRIGHT,STOP,CLEFT,CRIGHT,BACKUP};

	//Results Storage for sending to biorules
	std::vector<std::vector<cv::Point>> cLarva; //the detected larva contour (raw points)
	std::vector<int> sampleInd; //Integer sample index of the frame 
	std::vector<int> sampleTime; //Sample time (index multiplied by frame interval in ms)
	std::vector<cv::Point2f> head; //Head location (pixels)
	std::vector<cv::Point2f> headVelocity; //head velocity vector (pixels/s)
	std::vector<double> headSpeed; //head velocity amplitude (pixels/s)
	int headVelStep; //number of points back in history to search to calculate velocity
	std::vector<cv::Point2f> neck; //Neck location (pixels)
	std::vector<cv::Point2f> tail; //tail location (pixels)
	std::vector<cv::Point2f> tailVelocity; //tail velocity vector (pixels/s)
	std::vector<double> tailSpeed; //tail velocity amplitude (pixels/s)
	int tailVelStep; //number of points back in history to search to calculate velocity
	std::vector<double> length; //Skeleton length (pixels)
	std::vector<cv::Point2f> centroid; //contour centroid location (pixels)
	std::vector<double> headToBodyAngle; //Angle amplitude of headcast (radians)
	std::vector<double> tailBearingAngle; //tail bearing in image (radians)
	std::vector<cv::Rect> bBox; //bounding box 
	std::vector<std::vector<std::vector<double>>> fourier;  //Fourier descriptors for the contour
	std::vector<cv::Point> stagePos;  //Asynchronous Position of the stage (filled by stimThread)
	
	//will be about 10 hours of 1s position updates for full drawing
	//Assume that a position will be filled into this variable at a longer interval than 1/frame
	//This is for visualization of the animal's global track in the full arena
	std::vector<cv::Point> fullTrack;
	std::vector<double> fullTrackStim;
	int fullTrackInd; double binStimMax;
	
	//Tail bearing based on fitting a line to the points of the skeleton near the tail
	std::vector<double> newTailBearing;
	std::vector<double> dNewTailBearing; //Smoothed derivative of new tail bearing
	std::vector<double> newHeadBearing; //not implemented yet
	
	
	//Stimulus variables populated by stimThread
	std::vector<QString> stimCode; //Store the actual stimulus command string sent to the stimulus unit
	std::vector<BehaviorState> behaviorState; //Animal behavior state (uses ENUM defined above)
	std::vector<double> stimAmps; //Amplitude of stimulus (first pulse amplitude in waveform)
	
	//Performance
	std::vector<double> loopTime; //filled by stim thread, absolute number returned from embedded system
	std::vector<double> analysisTime; //filled by cam thread, profiled in software
	std::vector<double> networkTime; //filled by stim thread, profiled in software

	//Algorithm properties and constants
	int threshold; //binary threshold level, filled by Otsu method on first frame
	int nFourier;  //Number of fourier components of contour to resolve
	int fitRes;    //Number of points in the reconstructed perimeter
	double neckPercentage; //percentage of distance along the skeleton from the head defines neck location

	//The Master Analysis Function
	void larvaFind(uchar * img, int imWidth, int imHeight, int frameInd);


	//Local functions and local variables for the analysis
	cv::Mat input;
	cv::Mat output;
	cv::Mat dist;

	//profiling variables
	std::vector<double> profile;  //performance measures
	LARGE_INTEGER pcTic, pcToc, pcFreq;
	void tic();
	double toctic();

	std::vector<std::vector<cv::Point>> contours; //list of contours discovered
	std::vector<cv::Point2f> cFit; //rebuilt perimeter
	std::vector<double> curve; //curvature of the resulting fit
	
	//For head tail determination
	std::vector<cv::Point2f> headTail;  //Order of points (by curvature) found in THIS frame
	std::vector<cv::Point2f> masterHeadTail;  //Order of points found in first frame
	std::vector<int> votesHT; //votes for masterHeadTail
	std::vector<double> distHT;  //For distance based identification of points from frame to frame
	
	//for Skeleton
	int curveHeadInd; 
	std::vector<cv::Point2f> cFitSortedCW;  //points sorted from head to tail clockwise
	std::vector<cv::Point2f> cFitSortedCCW; //points sorted from head to tail counterclockwise
	std::vector<cv::Point2f> skeleton;  //Points along the skeleton (not stored from frame to frame)

	//Analysis Methods called in larvaFind

	/* Calculates an optimal threshold based on 2 class assumption - operates on the first frame only and sets threshold for remaining frames*/
	int otsuThreshold(uchar *img, int imPixelCount);
	
	/* Given a contour, fit the raw contour points with a low order set of fourier descriptors */
	void fourierDecompose(std::vector<cv::Point> contour, int nComponents, std::vector<std::vector<double>> & coeffs);
	enum fourCoef {AX = 0, BX = 1, AY = 2, BY = 3};
	
	/* Reconstruct a perimeter using the calculated fourier coefficients */
	void fourierReconstruct(std::vector<std::vector<double>> coeffs, std::vector<cv::Point2f> & cFit, int nPoints);
	
	/* Calculate internal angle curvature along a perimeter by looking curveDist ahead and back from each point and calculating the angle */
	void perimeterCurvature(std::vector<cv::Point2f> & cFit, std::vector<double> & curve, int curveDist);
	
	/* Given curvature data from the contour, determine animal head/tail by applying the head to the point that is generally sharpest curvature
			apply tail ID to second sharpest peak (as determined by votes over time)*/
	void findHeadTail(std::vector<cv::Point2f> & cFit, std::vector<double> crv, std::vector<cv::Point2f> & headTail);
	
	/* Given head/tail points, calculate skeleton by walking from head to tail, CW and CCW, taking midpoints */
	void skeletonCalc(std::vector<cv::Point2f> & cFit, std::vector<cv::Point2f> & skeleton, std::vector<cv::Point2f> & headTail, double & skelLen, cv::Point2f & neck);
	/* Developing an alternative algorithm to calculate skeleton using an erotion approach or a medial axis of the fourier polygon */
	void skeletonCalcDist();
	
	/* Determine the body bearing and head cast angle of the animal based on neck, and head/tail locations */
	void bodyAngles(double & tailBearing, double & head2BodyAngle, cv::Point2f head,cv::Point2f neck, cv::Point2f tail);
	
	/* Calculate velocities of head/tail/neck/etc */
	void calcVelocities(int nextInd);
};

#endif