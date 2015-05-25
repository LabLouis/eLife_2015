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
	CAMVIEWGL.H - OpenGL based visualization of camera image stream
		- Draws new frames as they are acquired
		- Draws crosshairs on image with 1mm spacing and center on center of image
		- If experiment is active:
			- Draws analysis results overlay
			- Draws stimulus visualization (blue blended overlay) on video if stimulating
			
*/
#ifndef CAMVIEWGL_H
#define CAMVIEWGL_H

#include <QGLWidget>
#include <QtOpenGL>
class Wormsign;


class CamViewGL : public QGLWidget
{
	Q_OBJECT

public:
	CamViewGL(QWidget * parent = 0);
	~CamViewGL();

	Wormsign * gui;

	//OpenGL based drawing functions
	void initializeGL();
	void resizeGL(int width, int height);
	void paintGL();

	//Custom draw functions called by paintGL
	void drawAnalysisOverlay();
	void drawCrosshairs();
	bool crosshair;

	//void mousePressEvent(QMouseEvent *event);
	//void mouseMoveEvent(QMouseEvent *event);
	//void mouseReleaseEvent(QMouseEvent *event);
	//void keyPressEvent(QKeyEvent *event);
	void closeEvent(QCloseEvent* event);

public:
	int fWidth, fHeight;
	QPoint topLeft; //Top left of camera ROI.  Draw coordinate system transformed to this reference point


};

#endif // CAMVIEWGL_H
