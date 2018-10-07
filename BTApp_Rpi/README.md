# Author: Pranav Gargeshwari

## FOLLOW Below commands to build the module
The idea is to build the src code on laptop than on the Raspberry pi itself

### Initial Setup:(MUST)
Make sure to edit the pi.cmake file as per your installed path where the SDK tools of Rpi reside.
Later follow the below instructions

1. First create a build folder along side of src folder.
```
mkdir build
```
2. Change the directory and move to build folder.
```
cd build
```
3. Run the below command
```
cmake . -DCMAKE_TOOLCHAIN_FILE=pi.cmake
```
4. Now run the make command. pass -j*2*No of CPU core for faster build
```
make
```
5. run make clean when required.
```
make clean
```
6. run make cleanall when required.
```
make cleanall
```