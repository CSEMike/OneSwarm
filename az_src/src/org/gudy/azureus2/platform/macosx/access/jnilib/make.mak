SDK = /Developer/SDKs/MacOSX10.4u.sdk
ARCHS = -arch i386 -arch ppc
LFLAGS = -bundle -isysroot $(SDK) $(ARCHS) -framework JavaVM -framework Carbon 
CFLAGS = -c $(ARCHS) -I $(SDK)/System/Library/Frameworks/JavaVM.framework/Headers -I $(SDK)/System/Library/Frameworks/ApplicationServices.framework/Versions/A/Frameworks/AE.framework/Versions/A/Headers


OBJECTS = OSXAccess.o
OUTLIB = libOSXAccess.jnilib

all:
	cc $(CFLAGS) OSXAccess.c
	cc -o $(OUTLIB) $(LFLAGS) $(OBJECTS)
