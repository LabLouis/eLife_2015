##Wormsign
####Closed Loop Larva Virtual-Olfaction-Reality System

This software is responsible for the real-time: 

    1) Image analysis with larvae tracking,
    2) Light stimulus delivery, and 
    3) Data acquisition.

######Software Developed By:
Gus K Lott III, PhD, PMP
lottg@janelia.hhmi.org
April 2011

######Software Developed At:
HHMI Janelia Research Campus
19700 Helix Dr.
Ashburn, VA, USA 20147

######Larva Tracker Project Team:
Science: Matthieu Louis, Vivek Jayaraman, Alex Gomez-Marin, Vani Rajendran
Engineering: Gus Lott, Eric Trautman, Lakshmi Ramasamy, Magnus Karlsson, Chris Werner, Pete Davies

For bug reports/comments/feedback, e-mail: mlouis_at_crg.eu or matthieu.louis_at_icloud.com

MAIN.CPP function launches the application GUI (defined in wormsign.h) and sets the windows priority class to REALTIME.  It then initializes the Qt event monitoring loop (in QApplication) which handles button-presses and graphics draw updates, etc.

###The application is structured as several concurrent processes:

    1) Main GUI Thread - defined in wormsign.h
    2) Camera Interface Thread - captures images and does analysis
    3) Stimulus Thread - monitors camera thread for analysis results and sends stimulus
    4) Stage Thread - Communicates with Zaber stages to keep track of current stage position and to track the animal
    5) Experiment Agent Thread - Coordinates the execution of the experiment and carries out data logging

###Wormsign must interconnect with the following systems:

    1) "Venkman" biorules server application (network socket)
    2) Four Zaber Stages (single serial interface)
    3) Stimulus delivery and Camera triggering embedded system (single serial interface)
    4) Basler A622fm Video Camera (IEEE 1394 w/ Basler Pylon Drivers installed)

###This application relies upon the following external software libraries:

    1) Developed under Windows 7 using the windows SDK
    2) Qt v4.7 Application Development Framework
    3) OpenCV v2.2 Computer Vision Library
    4) Basler Pylon v2.3 Camera Interface API
