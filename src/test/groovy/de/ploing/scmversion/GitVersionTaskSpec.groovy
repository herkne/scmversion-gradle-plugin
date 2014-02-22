package de.ploing.scmversion

import de.ploing.scmversion.git.GitVersionPlugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * @author Stefan Schlott
 */
class GitVersionTaskSpec extends Specification {
    static File testRepoDir

    def setupSpec() {
        testRepoDir = new File('./build/testrepos').absoluteFile
        if (!testRepoDir.exists()) {
            testRepoDir.mkdirs()
        }
        ['linearrepo', 'snapshotrepo'].each { name ->
            Tools.extractZip(getClass().classLoader.getResourceAsStream("${name}.zip"), testRepoDir)
        }
    }

    def "Versions are sorted correctly"() {
        setup:
            def v1 = [1,3,5,2]
            def v2 = [1,3,6,0]
            def v3 = [1,4,1]
            def v4 = [1,4,1,0]
        when:
            def compV1V2 = SCMVersionPluginExtension.compareVersions(v1, v2)
            def compV2V1 = SCMVersionPluginExtension.compareVersions(v2, v1)
            def compV2V3 = SCMVersionPluginExtension.compareVersions(v2, v3)
            def compV3V2 = SCMVersionPluginExtension.compareVersions(v3, v2)
            def compV3V4 = SCMVersionPluginExtension.compareVersions(v3, v4)
        then:
            compV1V2 < 0
            compV2V1 > 0
            compV2V3 < 0
            compV3V2 > 0
            compV3V4 == 0
    }

    def "Version of repo on revision tag is set correctly"() {
        setup:
            Project project = ProjectBuilder.builder().withProjectDir(new File(testRepoDir, 'linearrepo')).build()
        when:
            project.apply plugin: GitVersionPlugin
            project.scmversion {
                releaseTagPattern = 'rev-([0-9.]*)'
            }
            project.tasks.setVersion.setVersion()
        then:
            project.version=='1.0'
    }

    def "Snapshot version is set correctly"() {
        setup:
            Project project = ProjectBuilder.builder().withProjectDir(new File(testRepoDir, 'snapshotrepo')).build()
        when:
            project.apply plugin: GitVersionPlugin
            project.scmversion {
                releaseTagPattern = 'rev-([0-9.]*)'
            }
            project.tasks.setVersion.setVersion()
        then:
            project.version=='1.1-SNAPSHOT'
    }

    def "Property file of Snapshot version is written correctly"() {
        setup:
            Project project = ProjectBuilder.builder().withProjectDir(new File(testRepoDir, 'snapshotrepo')).build()
            File propFile = new File(testRepoDir, 'snapshotrepo/build/resources/main/scminfo.properties')
        when:
            project.apply plugin: GitVersionPlugin
            project.scmversion {
                releaseTagPattern = 'rev-([0-9.]*)'
                propertyFilename = propFile.name
            }
            project.tasks.createVersionFile.createVersionFile()
        then:
            propFile.exists()
            Properties props = new Properties()
            def inStream = new FileInputStream(propFile)
            props.load(inStream)
            inStream.close()
            props.getProperty('version')=='1.1-SNAPSHOT'
            props.getProperty('dirty')=='false'
    }
}
