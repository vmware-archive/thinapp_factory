Set wmi = GetObject("winmgmts:{impersonationLevel=impersonate,(Backup, Security)}!" _
                    & "\\.\root\cimv2")
Set files = wmi.ExecQuery("Select Name, FileName, LogfileName, Extension" _
                          & " From Win32_NTEventLogFile" _
                          & " Where LogfileName = 'Application'" _
                          & " OR LogfileName = 'System'" _
                          & " OR LogfileName = 'Security'")
Set fsObj = CreateObject("Scripting.FileSystemObject")
folder = WScript.Arguments.Item(0)
If Not fsObj.FolderExists(folder) Then
    fsObj.CreateFolder(folder)
End If
Set listfile = fsObj.CreateTextFile(folder & "filelist.txt", True)
For Each file in files
    backupFile = file.FileName & "." & file.Extension

    If fsObj.FileExists(folder & backupFile) Then
        fsObj.deleteFile folder & backupFile, True
    End If

    errBackupLog = file.BackupEventlog(folder & backupFile)
    If errBackupLog = 0 Then
        listfile.WriteLine file.LogfileName & ": " & backupFile
    Else
        WScript.Echo "Failure on backuping " & file.LogfileName & ", error = " & errBackupLog
    End If
Next
listfile.Close
