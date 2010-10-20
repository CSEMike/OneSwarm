SDK = /Developer/SDKs/MacOSX10.5.sdk
ARCHS = -arch x86_64
LFLAGS = -bundle -isysroot $(SDK) $(ARCHS) -framework JavaVM -framework Carbon
CFLAGS = -c $(ARCHS) -I $(SDK)/System/Library/Frameworks/JavaVM.framework/Headers -I $(SDK)/System/Library/Frameworks/ApplicationServices.framework/Versions/A/Frameworks/AE.framework/Versions/A/Headers


OBJECTS = OSXAccess.o
OUTLIB = libOSXAccess.jnilib

all:
	cc $(CFLAGS) OSXAccess.c
	cc -o $(OUTLIB) $(LFLAGS) $(OBJECTS)
