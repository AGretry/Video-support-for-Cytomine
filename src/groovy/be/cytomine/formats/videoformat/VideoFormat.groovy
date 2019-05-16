package be.cytomine.formats.videoformat

import be.cytomine.formats.Format

abstract class VideoFormat extends Format
{
    abstract public Thread convert()

    public int getNbFrames()
    {
        //Get number of frames from video
        def command = ["ffprobe -select_streams v -show_streams ${absoluteFilePath} 2>/dev/null | grep nb_frames | sed -e 's/nb_frames=//'"]
        def proc = command.execute()
        proc.waitFor()
        String stdout = proc.in.text
        int frames

        try
        {
            frames = Integer.parseInt(stdout)
        }
        catch (NumberFormatException e)
        {
            frames = -1
        }
        return frames
    }
}
