package be.cytomine.formats.videoformat

import utils.FilesUtils

class MP4Format extends VideoFormat
{

    public boolean detect()
    {
        boolean detect = false

        def command = ["ffprobe -select_streams v -show_format ${absoluteFilePath} 2>/dev/null | grep format_name | sed -e 's/format_name=//'"]
        def proc = command.execute()
        proc.waitFor()
        String stdout = proc.in.text

        for (String val: stdout.split(","))
            if (val == "mp4")
                detect = true

        return detect
    }

    @Override
    public Thread convert()
    {
        //Launch FFMPEG extraction in a thread
        def command = ["ffmpeg -i ${absoluteFilePath} -an ${absoluteFilePath}%d.jpg"]
        Thread tFFMPEG = new Thread(new Runnable()
        {
            public void run()
            {
                def proc = command.execute()
                proc.waitFor()
                if (proc.exitValue())
                {
                    String stderr = proc.err.text
                }
                String stdout = proc.in.text
            }
        })
        tFFMPEG.start()
        return tFFMPEG
    }
}
