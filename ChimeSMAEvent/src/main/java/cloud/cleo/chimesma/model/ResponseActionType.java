package cloud.cleo.chimesma.model;

/**
 *  All the action types supported by Chime PSTN Audio Service
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
    // Call Updates
    CallUpdateRequest
}
