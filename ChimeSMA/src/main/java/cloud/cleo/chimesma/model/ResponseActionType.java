/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package cloud.cleo.chimesma.model;

/**
 *
 * @author sjensen
 */
 public enum ResponseActionType {
    PlayAudio,
    PlayAudioAndGetDigits,
    Speak,
    SpeakAndGetDigits,
    Pause, 
    Hangup, 
    SendDigits, 
    ReceiveDigits, 
    CallAndBridge, 
    StartBotConversation,
    RecordAudio,
    // Call Recording 
    StartCallRecording,
    StopCallRecording,
    PauseCallRecording,
    ResumeCallRecording,
    // Chime Meetings
    JoinChimeMeeting,
    ModifyChimeMeetingAttendees,
}
