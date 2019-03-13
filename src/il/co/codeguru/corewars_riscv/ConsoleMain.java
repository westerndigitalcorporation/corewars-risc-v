package il.co.codeguru.corewars_riscv;

import il.co.codeguru.corewars_riscv.features.Feature;
import il.co.codeguru.corewars_riscv.features.FeatureSet;
import il.co.codeguru.corewars_riscv.gui.PlayersPanel;
import il.co.codeguru.corewars_riscv.utils.Logger;
import il.co.codeguru.corewars_riscv.war.Competition;
import il.co.codeguru.corewars_riscv.war.WarriorGroup;
import il.co.codeguru.corewars_riscv.war.WarriorRepository;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ConsoleMain {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: " + System.getProperty("sun.java.command").split(" ")[0] + " path-to-warriors");
            System.exit(0);
        }
        Logger.outputToStdout();

        Properties config = loadConfig("config.properties");
        List<PlayersPanel.Code> binaries = loadBinaryFiles(args[0]);

        Competition competition = setupCompetition(binaries, config);

        try {
            while (competition.continueRun())
            { }

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        for(WarriorGroup group : competition.getWarriorRepository().getWarriorGroups())
        {
            System.out.println(group.getName() + ":" + group.getGroupScore());
        }

        outputRepoToFile(
                competition.getWarriorRepository(),
                config.getProperty("OUTPUT_FILE")
        );
    }



    public static Competition setupCompetition(List<PlayersPanel.Code> binaries, Properties config)
    {
        Competition competition = new Competition();
        boolean ok = competition.getWarriorRepository().loadWarriors(
                binaries.toArray(new PlayersPanel.Code[0]),
                new PlayersPanel.Code[]{},
                false
        );
        if(!ok) throw new RuntimeException("Failed to load warriors");

        FeatureSet features = FeatureSet.getAllFeatures();
        features.getRegisterdFeatures().stream()
                .filter(e -> config.getProperty(e.getKey(), "false").equals("true"))
                .forEach(e -> e.getValue().enable());

        competition.runCompetition(
                100,
                competition.getWarriorRepository().getNumberOfGroups(),
                false,
                features.getEnabledFeatures()
        );
        return competition;
    }

    /**
     * Directory structure:
 *      root_path:
 *          team_name1:
 *              code1.bin
 *              code2.bin
 *          team_name2:
 *              ...
 *          ...
     */
    public static List<PlayersPanel.Code> loadBinaryFiles(String path) {
        File folder = new File(path);
        File[] teamDirList = folder.listFiles();
        if (teamDirList == null) {
            System.out.println(path + " is not a valid path or doesn't exist");
            System.exit(1);
        }
        List<PlayersPanel.Code> ans = new ArrayList<>(teamDirList.length);


        for (File dir : teamDirList) {
            PlayersPanel.PlayerInfo teamInfo = new PlayersPanel.PlayerInfo(dir.getName(), dir.getName());
            File[] codeList = dir.listFiles();
            if (codeList != null) {
                for (int codeIndex = 0; codeIndex < codeList.length; codeIndex++) {
                    File codeFile = codeList[codeIndex];
                    if(codeFile.getName().endsWith(".bin")) {
                        try {
                            byte[] code = Files.readAllBytes(codeFile.toPath());
                            PlayersPanel.Code codeObject = new PlayersPanel.Code(teamInfo, codeIndex);
                            codeObject.bin = code;
                            codeObject.name = teamInfo.getName();
                            ans.add(codeObject);
                        } catch (IOException e) {
                            e.printStackTrace(System.err);
                        }
                    }
                }
            }
        }
        return ans;
    }

    public static void outputRepoToFile(WarriorRepository repository, String filename)
    {
        System.err.println("Writing results to: " + filename);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

            for(WarriorGroup group : repository.getWarriorGroups())
            {
                writer.write(group.getName() + ":" + group.getGroupScore());
                writer.newLine();
            }
            writer.close();
        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }

    }


    public static Properties loadConfig(String filename)
    {
        Properties config = new Properties();
        FileInputStream is;
        try {
            is = new FileInputStream(filename);
        } catch (FileNotFoundException ex) {
            System.err.println("WARNING: Config not found!");
            return config;
        }
        try {
            config.load(is);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return config;
    }
}

