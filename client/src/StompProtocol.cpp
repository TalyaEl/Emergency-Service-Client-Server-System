#include "../include/StompProtocol.h"
#include "../include/ConnectionHandler.h"
#include <iostream>
#include <string>
#include <vector>
#include <set>
#include "/workspaces/Assignment3/client/src/ConnectionHandler.cpp"
using namespace std;
// const int connectionId;// conection id
// const string username; //current username
// bool isLoggedIn; // true if user is logged in
// int nextSubsctiptionId; // counter to generate next subscriprion id
//std::map<string,int > channelSubs; // hashmap for <channel, subscriptionId>
// std::vector<Event> events;
enum Command {
    login,
    join,
    exitChannel,
    report,
    logout,
    summary
};

Command stringToCommand(const std::string& command) {
    if (command == "login") return login;
    if (command == "join") return join;
    if (command == "exit") return exitChannel;
    if (command == "report") return report;
    if (command == "logout") return logout;
    if (command == "summary") return summary;
}

StompProtocol::StompProtocol(int Id, string name): 
handler(),connectionId(Id),username(name),isLoggedIn(false),
nextSubsctiptionId(1),channelSubs(),myChannels(),events()
{}

void StompProtocol::frameNav(const std::vector<string>& args){
        string frameType= args[0];
        switch (stringToCommand(frameType)) {
            case Command::login:
                if(args.size()<=5){
                    login(args[1],args[2],args[3],args[4]);
                }
                else{
                    cout << "login command needs 3 args: {host:port} {username} {password}";
                }
                break;
            case Command::join:
                if(args.size()<=2){
                    join(args[1]);
                }
                else{
                    cout << "join command needs 1 args: {channel_name}";
                }
                break;
            case Command::exitChannel:
                if(args.size()<=2){
                    exit(args[1]);
                }
                else{
                    cout << "exit command needs 1 args: {channel_name}";
                }
                break;
            case Command::report:
                if(args.size()<=2){
                    report(args[1]);
                }
                else{
                    cout << "report command needs 1 args: {file}";
                }
                break;
            case Command::logout:
                logout();
                break;
            case Command::summary:
                if(args.size()<=3){
                    summary(args[1], args[2], args[3]);
                }
                else{
                    cout << "summary command needs 3 args: {channel_name} {user} {file}";
                }
                break;
            default:
                cout << "invalid command";
                break;
        }
}


bool StompProtocol::login(string host, string port, string username, string password){
    short myPort= std::stoi(port);
    bool ans;
    if(isLoggedIn==true){
        cout << "The client is already logged in, log out before trying again";
        return false;
    }
    else{
        isLoggedIn=true;
        this->handler= new ConnectionHandler(host, myPort);
        ans=handler->connect();
        if(ans==true){
            cout << "Login succesful";
            return true;
        }
        else return false;
    }
}

bool StompProtocol::join(string channel){
    if(isSubscribed(channel)){     //check correct input data
        cout << "cannot subscribe to a channel you are already subscribed to";
        return false;
    }
    
    string frame = "SUBSCRIBE\n" + string("destination:") + channel + "\n" + "id:"
     + to_string(nextSubsctiptionId) + "\n" +"\0"; //create subscription frame//check if needed a recipt frame;    
    bool ans= handler->sendFrameAscii(frame,'\0'); // send using handler.sendFrameAscii
    if(ans){
        myChannels.push_back(channel);
        channelSubs.insert({channel,nextSubsctiptionId});
        nextSubsctiptionId++;
        cout << "Joined channel " + channel;
        return true;
    }
    return false;
    //return if was sent
    //if was sent:
    //raise sub id ++
    //add to mychannles
    //add to channlesubs
    //syso "Joined channel <channelname>"
}
bool StompProtocol::exit(string channel){
    if(!isSubscribed(channel)){//check correct input data
        cout << "you are not subscribed to the channel";
        return false;
    }
    int subId = channelSubs.find(channel);
    string ans= "UNSUBSCRIBE \n" + 

    //create unsubscribe frame
    // send using  handler.sendFrameAscii()
    //if sent correctly :
    //remove to mychannles
    //remove to channlesubs
    //syso "Exited channel <channelname>"

}
bool StompProtocol::report(string message){
    //check correct input data
    //check if i am subscribed to channel if not syso "not subscribed to chanel"
    // create message frame
    // send using  handler.sendFrameAscii()
    // if sent correctly syso "Reported"
}
bool StompProtocol::logout(){
    //check correct input data
    // check if isrunning == true
    //change isrunnig to false
    //send dissconnect frame
    //call destructor
}
bool StompProtocol::summary(string channel, string user ,string txtName){
    //check correct input data
    //check if subscribed to channel
    //create txt file
}

bool StompProtocol::isSubscribed(string channel){
    for( string c: myChannels){
        if(c== channel)
            return true;
    }
    return false;
}


// //bool StompProtocol::processFrame(StompFrame frame);


