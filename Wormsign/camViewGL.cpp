#include "camViewGL.h"
#include "wormsign.h"
#include "experimentAgent.h"
#include "analysisModule.h"
#include "stimThread.h"

CamViewGL::CamViewGL(QWidget * parent):QGLWidget(parent){

	gui = NULL;

	setGeometry(100,100,1280,1024);
	setMaximumHeight(1024);
	setMinimumHeight(1024);
	setMaximumWidth(1280);
	setMinimumWidth(1280);
	setWindowTitle("Wormsign - Visualization Canvas");
	crosshair = true;
};
CamViewGL::~CamViewGL(){};

void CamViewGL::initializeGL(){

	fWidth = 1280;
	fHeight = 1024;
	qglClearColor(Qt::black);
	glViewport(0,0,fWidth,fHeight);
	glMatrixMode(GL_MODELVIEW);

	glEnable(GL_TEXTURE_2D);
	glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
	glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER, GL_LINEAR);

};
void CamViewGL::closeEvent(QCloseEvent* event){
	qApp->quit();
};
void CamViewGL::resizeGL(int width, int height){

	fWidth = width;
	fHeight = height;

	glViewport(0,0,geometry().width(),geometry().height());
	glMatrixMode(GL_PROJECTION);
	glLoadIdentity();
	glOrtho(0,1280,1024,0,1.0,-1.0); //flipped up/down from standard GL to match Qt view
	glMatrixMode(GL_MODELVIEW);
	glLoadIdentity();


};
void CamViewGL::paintGL(){

	glClear(GL_COLOR_BUFFER_BIT);  //Clear frame to background color (start fresh)
	glMatrixMode(GL_MODELVIEW);

	bool analyzing = (gui->expAgent != 0);

	//return if no activity
	if(!gui->camThread) return;
	if(gui->camThread->stopped) return;
	if(gui->camThread->frameCount == -1) return; //if no frames captured yet
	if(analyzing) if(gui->expAgent->analysisMod->index == -1) return; //if analyzing and no results are available

	//get at the appropriate image for the available data
	int ind = gui->camThread->frameCount % gui->camThread->imBuffer.size();
	if(analyzing) ind = gui->expAgent->analysisMod->index % gui->camThread->imBuffer.size(); //should always be the same or lagging

	uchar * imgSrc = gui->camThread->imBuffer[ind];
	int imWidth = gui->camThread->width;
	int imHeight = gui->camThread->height;
	topLeft = QPoint(gui->camThread->left,gui->camThread->top);
	double xval,yval;

	glLoadIdentity();
	glTranslatef(topLeft.x(),topLeft.y(),0); //shift all subsequent drawings to image coordinates

	//Draw the image at the appropriate ROI shifted location
	glEnable(GL_TEXTURE_2D);
	glTexImage2D(GL_TEXTURE_2D,0,GL_LUMINANCE,imWidth,imHeight,0,GL_LUMINANCE,GL_UNSIGNED_BYTE,imgSrc); //upload texture

	glColor4f(1.0,1.0,1.0,1.0);
	glBegin(GL_QUADS);  //Draw simple quad on which to map the texture
		glTexCoord2f(0,0);
		xval = 0;
		yval = 0;
		glVertex2f(xval,yval);

		glTexCoord2f(1,0);
		xval = imWidth;
		yval = 0;
		glVertex2f(xval,yval);
		
		glTexCoord2f(1,1);
		xval = imWidth;
		yval = imHeight;
		glVertex2f(xval,yval);

		glTexCoord2f(0,1);
		xval = 0;
		yval = imHeight;
		glVertex2f(xval,yval);
	glEnd();

	glDisable(GL_TEXTURE_2D);

	//Draw Analysis Results Overlaid on Image
	if(analyzing)
		drawAnalysisOverlay();

	//Draw Centered Dot
	glPointSize(5);
	glColor3f(1.0,0,0);
	glBegin(GL_POINTS);
		xval = double(imWidth)/2.0;
		yval = double(imHeight)/2.0;
		glVertex2f(xval,yval);
	glEnd();

	if(crosshair) drawCrosshairs();
};

void CamViewGL::drawCrosshairs(){

	double xval, yval;
	double imWidth = gui->camThread->width;
	double imHeight = gui->camThread->height;

	//Draw Crosshairs
	glLineWidth(1);
	glColor3f(1.0,0,0);
	glBegin(GL_LINES);
		xval = imWidth/2.0;
		yval = 0;
		glVertex2f(xval,yval);
		yval = imHeight;
		glVertex2f(xval,yval);
	glEnd();
	glBegin(GL_LINES);
		xval = 0;
		yval = imHeight/2.0;
		glVertex2f(xval,yval);
		xval = imWidth;
		glVertex2f(xval,yval);
	glEnd();

	//draw ticks at 1mm based on scale radiating out from center
	//for horizontal axis
	double pxPerMM = 1000.0/gui->camThread->umPerPixel;
	xval = imWidth/2.0;
	while(xval>0) xval-=pxPerMM;
	xval+= pxPerMM;
	glBegin(GL_LINES);
		while(1){
			yval = imHeight/2.0 + 10.0;
			glVertex2f(xval,yval);
			yval = imHeight/2.0 - 10.0;
			glVertex2f(xval,yval);
			xval += pxPerMM;
			if(xval > imWidth) break;
		}
	glEnd();
	//for vertical Axis
	yval = imHeight/2.0;
	while(yval>0) yval-=pxPerMM;
	yval+= pxPerMM;
	glBegin(GL_LINES);
		while(1){
			xval = imWidth/2.0 + 10.0;
			glVertex2f(xval,yval);
			xval = imWidth/2.0 - 10.0;
			glVertex2f(xval,yval);
			yval += pxPerMM;
			if(yval > imHeight) break;
		}
	glEnd();	

};

void CamViewGL::drawAnalysisOverlay(){
	
	AnalysisModule * aMod = gui->expAgent->analysisMod;
	int ind = aMod->index % aMod->nBuffers;
	double xval,yval;
	glLineWidth(1);

	//draw raw contour
	std::vector<cv::Point> cLarva = aMod->cLarva[ind];
	glColor3f(0.7,0.7,0.0);
	glBegin(GL_LINE_LOOP);
		for(int i=0; i<cLarva.size(); i++){
			glVertex2f(cLarva[i].x,cLarva[i].y);
		}
	glEnd();

	//draw reconstructed fourier contour
	std::vector<cv::Point2f> cFit;
	aMod->fourierReconstruct(aMod->fourier[ind],cFit,200);

	glLineWidth(2);
	glColor3f(0.0,0.7,0.0);
	glBegin(GL_LINE_LOOP);
		for(int i=0; i<cFit.size(); i++){
			glVertex2f(cFit[i].x,cFit[i].y);
		}
	glEnd();

	//Draw centroid
	glColor3f(1.0,1.0,1.0);
	glPointSize(7);
	glBegin(GL_POINTS);
		xval = aMod->centroid[ind].x;
		yval = aMod->centroid[ind].y;
		glVertex2f(xval,yval);
	glEnd();

	//Draw Head and Tail Points
	glPointSize(15);
	glColor3f(1.0,0.0,0.0);
	glBegin(GL_POINTS);
		xval = aMod->head[ind].x;
		yval = aMod->head[ind].y;
		glVertex2f(xval,yval);
	glEnd();
	renderText(xval+10+topLeft.x(),yval+topLeft.y(),QString("Head"));

	glColor3f(0.0,0.0,1.0);
	glBegin(GL_POINTS);
		xval = aMod->tail[ind].x;
		yval = aMod->tail[ind].y;
		glVertex2f(xval,yval);
	glEnd();


	//Draw Skeleton
	glColor3f(1.0,0,0);
	glBegin(GL_LINE_STRIP);
		for(int i=0; i<aMod->skeleton.size(); i++){
			xval = aMod->skeleton[i].x;
			yval = aMod->skeleton[i].y;
			glVertex2f(xval,yval);
		}
	glEnd();

	//draw bearing line projecting past neck
	glColor3f(1.0,1.0,0);
	//cvLine(output,cvPoint(int(midX-100.0*cos(tailAngle)),int(midY-100.0*sin(tailAngle))),tail,cvScalar(0,255,255),2); //tail to mid
	glBegin(GL_LINES);
		xval = aMod->neck[ind].x - 100.0*cos(aMod->tailBearingAngle[ind]);
		yval = aMod->neck[ind].y - 100.0*sin(aMod->tailBearingAngle[ind]);
		glVertex2f(xval,yval);
		xval = aMod->tail[ind].x;
		yval = aMod->tail[ind].y;
		glVertex2f(xval,yval);
	glEnd();
	

	//draw head bearing from neck to head
	
	glLineWidth(3);
	glBegin(GL_LINES);
		xval = aMod->neck[ind].x;
		yval = aMod->neck[ind].y;
		glVertex2f(xval,yval);
		xval = aMod->head[ind].x;
		yval = aMod->head[ind].y;
		glVertex2f(xval,yval);
	glEnd();

	//draw Neck Point
	glColor3f(1.0,1.0,0.4);
	glPointSize(10);
	glBegin(GL_POINTS);
		xval = aMod->neck[ind].x;
		yval = aMod->neck[ind].y;
		glVertex2f(xval,yval);
	glEnd();

	//Draw new Tail Bearing Line
	glColor3f(1.0,1.0,0.4);
	glBegin(GL_LINES);
		xval = aMod->tail[ind].x - 300.0*cos(aMod->newTailBearing[ind]);
		yval = aMod->tail[ind].y - 300.0*sin(aMod->newTailBearing[ind]);
		glVertex2f(xval,yval);
		xval = aMod->tail[ind].x;
		yval = aMod->tail[ind].y;
		glVertex2f(xval,yval);
	glEnd();




	//Visualization of stimulus
	//Draw a blue overlay with alpha blending proportional to intensity of flash
	int imWidth = gui->camThread->width;
	int imHeight = gui->camThread->height;
		
	int stimAmpInd = gui->stimThread->index % gui->stimThread->stimAmps.size();
	double stimAmp = gui->stimThread->stimAmps[stimAmpInd];
	//See if there was a stimulus in the last 4 frames
	if(stimAmpInd > 3)
		for(int i=1; i<4; i++)
			if(stimAmp < gui->stimThread->stimAmps[stimAmpInd-i])
				stimAmp = gui->stimThread->stimAmps[stimAmpInd-i];
		
	if(stimAmp > 0){
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
		glColor4f(0.0,0.0,1.0,stimAmp*.4/100.0);
		glBegin(GL_QUADS);  //Draw simple quad to blend over the image (indicate stimulus)
			xval = 0;
			yval = 0;
			glVertex2f(xval,yval);

			glTexCoord2f(1,0);
			xval = imWidth;
			yval = 0;
			glVertex2f(xval,yval);
			
			glTexCoord2f(1,1);
			xval = imWidth;
			yval = imHeight;
			glVertex2f(xval,yval);

			glTexCoord2f(0,1);
			xval = 0;
			yval = imHeight;
			glVertex2f(xval,yval);
		glEnd();
		glDisable(GL_BLEND);
	}


	
	//Plot Fourier Coefficient Amplitude
	//double ax,bx,ay,by,amp;
	//xval = 800;
	//yval = 1000;
	//for(int i=1; i<aMod->nFourier; i++){
	//	ax = aMod->fourier[ind][i][0];
	//	bx = aMod->fourier[ind][i][1];
	//	ay = aMod->fourier[ind][i][2];
	//	by = aMod->fourier[ind][i][3];
	//	
	//	amp = sqrt(pow(sqrt(ax*ax+bx*bx),2) + pow(sqrt(ay*ay+by*by),2));
	//	amp *= 5;
	//	glColor4f(1.0,0.0,1.0,1.0);
	//	glBegin(GL_QUADS);  //Draw simple quad on which to map the texture
	//		glVertex2f(xval,yval);
	//		glVertex2f(xval+50,yval);
	//		glVertex2f(xval+50,yval-amp);
	//		glVertex2f(xval,yval-amp);
	//	glEnd();
	//	xval += 70;
	//}
};