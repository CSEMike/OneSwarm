#import <Cocoa/Cocoa.h>

#define fprintf

NSMutableDictionary *map;
Boolean useNSWorkspace;

@interface IONotification : NSObject {
	IONotificationPortRef gNotifyPort;
}

-(void)setup;
-(void)mount:(id)notification;
-(void)unmount:(id)notification;
-(int)checkExisting;

- (void)rawDeviceAdded:(io_iterator_t)iterator;

//void rawDeviceAdded(void *refCon, io_iterator_t iterator);
void DeviceNotification(void *refCon, io_service_t service, natural_t messageType, void *messageArgument);

@end


@interface StatfsObject : NSObject
{
@public
	struct statfs *fs;
}
@end
