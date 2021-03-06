**********************************************
Odor_stimulation.vi: Execution of odor stimulation protocols to evoke neural activity according to a preconceived stimulation file (csv format).

Software Developed By:
Aljoscha Schulze
Center for Genomic Regulation (CRG), Barcelona, Spain
AljoschaTobias@yahoo.com

For bug reports/comments/feedback, e-mail: mlouis_at_crg.eu or matthieu.louis_at_icloud.com
**********************************************

This package contains a LabView routine to drive a dynamic air-flow/odor stimulation according to a preconceived odor stimulation time course that contains voltage values defining the corresponding to the air-flow in each channel. The stimulation execution is defined in such a way as to have the combined flow of both channels never exceed a predefined value. As a result the combined flow of both channels is kept constant.

This two channel stimulation protocol was used in conjunction with one 'NI USB-6008 12-Bit, 10 kS/s Low-Cost Multifunction DAQ' board connected to a PC. Each of the two analog outputs of this DAQ board was connected to an air-flow controller driving changes of the air-flow according to the loaded stimulation protocol. In order to learn the relationship between the output voltage and the achieved air-flow/odor concentration a calibration of the flow controller (by measuring the air-flow and/or the resulting odor concentration) has to be performed before running this routine. 

The odor stimulation protocol has to come in the form of a csv file containing one column of voltage values (corresponding the values driving the air-flow controller of the odor channel). Every line in this csv file corresponds to a voltage and thus a flow value of the odor stimulation (values should not exceed 5V). The frame-size (Set frame size) of the execution protocol has to be defined before the stimulation is executed at the LabView VI front panel by the user (units are in milliseconds).

**********************************************
Main routines:
**********************************************

* Odor_stimulation.vi: Two channel odor stimulation interface

Execution of this program leads to the execution of custom air-flow/odor stimuli via two channels of the analog output port of a Multifunction DAQ device. The stimulus time course (voltage values) has to be computed prior to the execution and saved as a csv file. The empty channel is continuously computed by subtracting the voltage values of the odor channel from the user defined maximum flow rate. The user should adapt the stimulation protocol (csv file) to the frame-size that he intends to use.

**********************************************
Front panel:
**********************************************

Stimulation START: Pressing this button leads to a prompt asking the user to load the stimulation csv file. As soon as the file is selected the stimulation starts.

Set frame size (min=2ms): Define the frame size of the stimulation protocol in units of milliseconds.

Set maximum voltage: Define the voltage corresponding to the combined flow of channel 1 + channel 2. As a result the flow will always be kept at this constant value throughout the stimulation.

Duration of the Stimulation (s): Shows the duration of the stimulation protocol for the defined frame-size.

Length of csv file: Shows the number of values (each values corresponding to a single frame of the stimulation) contained in the loaded stimulation csv file.

Format of the csv file: The csv file should contain only one column of numbers (voltage values) that define the flow rate of the odor-channel. The values driving the complementary (empty) channel are derived by subtracting the odor channel from the defined overall flow ('maximum voltage').

**********************************************
Block diagram:
**********************************************

The user has to open the DAQ Assistant and edit both the coordinates and the analog output port of the DAQ device that is being used to drive the light stimulation via the LED controller.

