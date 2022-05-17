#import <Foundation/Foundation.h>
#import <Cocoa/Cocoa.h>
#import <unistd.h>

BOOL copy_to_clipboard(NSString *path) {
  NSImage * image;
  if([path isEqualToString:@"-"]) {
    NSFileHandle *input = [NSFileHandle fileHandleWithStandardInput];
    image = [[NSImage alloc] initWithData:[input readDataToEndOfFile]];
  } else { 
    image =  [[NSImage alloc] initWithContentsOfFile:path];
  }
  BOOL copied = false;
  if (image != nil) {
    NSPasteboard *pasteboard = [NSPasteboard generalPasteboard];
    [pasteboard clearContents];
    NSArray *copiedObjects = [NSArray arrayWithObject:image];
    copied = [pasteboard writeObjects:copiedObjects];
    [pasteboard release];
  }
  [image release];
  return copied;
}

int main(int argc, char * const argv[]) {
  NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
  if (argc < 2) {
    return EXIT_FAILURE;
  }
  NSString *path= [NSString stringWithUTF8String:argv[1]];
  BOOL success = copy_to_clipboard(path);
  [pool release];
  return (success?EXIT_SUCCESS:EXIT_FAILURE);
}