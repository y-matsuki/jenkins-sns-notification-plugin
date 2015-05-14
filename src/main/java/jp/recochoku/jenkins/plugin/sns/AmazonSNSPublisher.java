package jp.recochoku.jenkins.plugin.sns;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

/**
 * Sample {@link Publisher}.
 * <p/>
 * <p/>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link AmazonSNSPublisher} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #firstName})
 * to remember the configuration.
 * <p/>
 * <p/>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked.
 *
 * @author Yuto Matsuki
 */
public class AmazonSNSPublisher extends Publisher {

  private final String topicArn;
  private final String region;
  private final String subject;
  private final String message;

  @DataBoundConstructor
  public AmazonSNSPublisher(String topicArn, String region, String subject, String message) {
    this.topicArn = topicArn;
    this.region = region;
    this.subject = subject;
    this.message = message;
  }

  public String getTopicArn() {
    return topicArn;
  }

  public String getRegion() {
    return region;
  }

  public String getSubject() {
    return subject;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
      throws InterruptedException, IOException {
    String topicArn = StringUtils.defaultIfBlank(getTopicArn(), getDescriptor().getTopicArn());
    String region = StringUtils.defaultIfBlank(getRegion(), getDescriptor().getRegion());

    // send notification
    listener.getLogger().println("AmazonSNS: " + topicArn + "(" + region + ")");
    listener.getLogger().println("Subject: " + getSubject());
    listener.getLogger().println("Message: " + getMessage());
    AWSCredentialsProvider cp = new DefaultAWSCredentialsProviderChain();
    AmazonSNS sns = new AmazonSNSClient(cp);
    sns.setRegion(Region.getRegion(Regions.fromName(region)));
    PublishResult result = sns.publish(new PublishRequest()
        .withTopicArn(topicArn).withSubject(subject).withMessage(message));
    listener.getLogger().println("Send message: " + result.getMessageId());
    return true;
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.STEP;
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  @Extension
  public static final class DescriptorImpl extends Descriptor<Publisher> {

    private String topicArn;
    private String region;

    public DescriptorImpl() {
      load();
    }

    public String getDisplayName() {
      return "Amazon SNS Notification";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
      topicArn = formData.getString("topicArn");
      region = formData.getString("region");
      save();
      return super.configure(req, formData);
    }

    public String getTopicArn() {
      return topicArn;
    }

    public String getRegion() {
      return region;
    }
  }

}

