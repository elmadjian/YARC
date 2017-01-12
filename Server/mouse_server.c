#include <stdio.h>
#include <strings.h>
#include <string.h>
#include <X11/Xlib.h>
#include <X11/extensions/XTest.h>
#include <sys/socket.h>
#include <netinet/in.h>

#define MOVE_C 1
#define LMOUSEDOWN_C 2
#define LMOUSEUP_C 3
#define LMOUSECLICK_C 4
#define RMOUSECLICK_C 5
#define SCROLLUP_C 6
#define SCROLLDOWN_C 7
#define CHARDOWN_C 8
#define KEYDOWN_C 9
#define UNKNOWN_C -1


void move (Display*, int, int);
void getCoords(Display*, int*, int*);
void click(Display*, int);
void mouseDown(Display*, int);
void mouseUp(Display*, int);
void charDown(Display *, char);
void keyDown(Display*, int);
int interpretCommand(char*);

/* global state variables */
/**************************/
/*bool LFTBUTTON = False;*/


/* main program */
/***************/
int main() {
    int x, y;
    int server_fd, client_fd;
    int optval, read;
    struct sockaddr_in server, client;
    char buff[1024];
    char command[3];
    char key;

    /*open display*/
    Display *display = XOpenDisplay(NULL);
    if (display == NULL) {
        fprintf(stderr, "Sorry, could not open display :[ \n");
        return -1;
    }

    /*create socket*/
    if ( (server_fd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        fprintf(stderr, "Sorry, could not create a server socket :[ \n]");
        return -1;
    }
    optval = 1;
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR,
        (const void *) &optval , sizeof(int));

    /*build address*/
    bzero((char *) &server, sizeof(server));
    server.sin_family = AF_INET;
    server.sin_port = htons(11111);
    server.sin_addr.s_addr = htonl(INADDR_ANY);

    /*bind socket to the chosen port*/
    if ( bind(server_fd, (struct sockaddr *) &server, sizeof(server)) < 0) {
        fprintf(stderr, "Sorry, port 11111 appears to be in use :[ \n");
        return -1;
    }

    /*listen to client*/
    if ( listen(server_fd, 10) < 0) {
        fprintf(stderr, "Sorry, could not listen to client :[ \n");
        return -1;
    }

    while(1) {
        socklen_t client_len = sizeof(client);
        if ( (client_fd = accept(server_fd, (struct sockaddr *)
            &client, &client_len)) < 0) {
            fprintf(stderr, "Sorry, could not establish connection :[ \n");
        }

        /* client loop */
        while (1) {
            bzero(buff, 1024);
            bzero(command, 3);
            read = recv(client_fd, buff, 1024, 0);
            if (!read) break;
            if (read < 0) {
                fprintf(stderr, "Sorry, there is an error with the connection...\n");
                break;
            }
            /*printf("server received %d bytes: %s\n", read, buff);*/
            sscanf(buff, "%s %c %d %d", command, &key, &x, &y);
            switch (interpretCommand(command)) {
                case MOVE_C:        move(display, x, y); break;
                case LMOUSEDOWN_C:  mouseDown(display, Button1); break;
                case LMOUSEUP_C:    mouseUp(display, Button1); break;
                case LMOUSECLICK_C: click(display, Button1); break;
                case RMOUSECLICK_C: click(display, Button3); break;
                case SCROLLUP_C:    click(display, Button5); break;
                case SCROLLDOWN_C:  click(display, Button4); break;
                case CHARDOWN_C:    charDown(display, key); break;
                case KEYDOWN_C:     keyDown(display, x); break;
                default: break;
            }
        }
    }

    XCloseDisplay(display);
    return 0;
}

/* move cursor to a (x,y) coordinate on screen */
/***********************************************/
void move (Display *display, int x, int y) {
    /* ABSOLUTE MOTION: XTestFakeMotionEvent(display, -1, x, y, CurrentTime);*/
    XTestFakeRelativeMotionEvent(display, x, y, CurrentTime);
    XFlush(display);
}


/* simulates a mouse click */
/*****************************/
void click(Display *display, int button) {
    XTestFakeButtonEvent(display, button, True, CurrentTime);
    XTestFakeButtonEvent(display, button, False, CurrentTime);
    XFlush(display);
}

/* simulates a mouse button press*/
/*********************************/
void mouseDown(Display *display, int button) {
    XTestFakeButtonEvent(display, button, True, CurrentTime);
    XFlush(display);
}

/* simulates a mouse button release*/
/***********************************/
void mouseUp(Display *display, int button) {
    XTestFakeButtonEvent(display, button, False, CurrentTime);
    XFlush(display);
}

/* simulates a key press event */
/*******************************/
void keyDown(Display *display, int keycode) {
    XTestFakeKeyEvent(display, keycode, True, CurrentTime);
    XTestFakeKeyEvent(display, keycode, False, CurrentTime);
    XFlush(display);
}

/* simulates a string input */
/****************************/
void charDown(Display *display, char key) {
    KeySym sym = XStringToKeysym(&key);
    KeyCode keycode = XKeysymToKeycode(display, sym);
    keyDown(display, keycode);
}

/* interpret remote command*/
/***************************/
int interpretCommand(char *input) {
    if ( strcasecmp(input, "mov") == 0)
        return MOVE_C;
    if ( strcasecmp(input, "lmd") == 0)
        return LMOUSEDOWN_C;
    if ( strcasecmp(input, "lmu") == 0)
        return LMOUSEUP_C;
    if ( strcasecmp(input, "lmc") == 0)
        return LMOUSECLICK_C;
    if ( strcasecmp(input, "rmc") == 0)
        return RMOUSECLICK_C;
    if ( strcasecmp(input, "scu") == 0)
        return SCROLLUP_C;
    if ( strcasecmp(input, "scd") == 0)
        return SCROLLDOWN_C;
    if ( strcasecmp(input, "chr") == 0)
        return CHARDOWN_C;
    if ( strcasecmp(input, "key") == 0)
        return KEYDOWN_C;
    return UNKNOWN_C;
}
