# mention-notifications-ejb
An EJB that reads and posts Github mention notifications.

More specifically, it checks for the logged-in user's notifications of type "mention" and posts them to a specified REST endpoint in simplified format. Only the ``repoFullName`` and ``issueNumber`` are sent - the receiver then has to implement the look-up logic in order to find and handle the proper mentioning comment. 

Other info returned by the Github API in a Notification object would be rather useless payload since it consists mostly of links that the receiver can build on its own knowing the repo name and issue number.

The main use of such a checker would be together with a Github bot account; naturally, the bot has to act upon received notifications. The bot implementation would have a rest POST endpoint to receive the notifications send by this checker.

I use this in one of my projects so far and will probably use it again with others. It was initially a part of said repository but I decided to pull it out and make it reusable.

BTW, I implement all the Github interaction using [this](https://github.com/jcabi/jcabi-github/) awesome library. Check it out, it also offers a mock version of the API so you can unit test your code instantly.

Why not [Github WebHooks](https://developer.github.com/webhooks/)?
A few reasons:
- When I first wrote it I didn't even know about the webhooks and when I had it written I decided that I'd rather have the checker configurable (you can configure this EJB to check at any interval of minutes) and extendable - a class can easely be added to also handle other type of notifications.
- I don't want the users of my bot to have to configure their repos and setup the hooks.
- Smaller load, since this checker only sends the required info.

You will need to set the following system properties. **Pay a lot of attention while configuring these, since everything relies on them**.

## EJB notifications checker sys props
<table>
  <tr>
    <th>Name</th><th>Value</th><th>Description</th>
  </tr>
  <tr>
    <td>checks.interval.minutes</td>
    <td>integer</td>
    <td><b>Optional</b>. Minutes that should <br> pass between checks. Defaults to 2.</td>
  </tr>
  <tr>
    <td>post.rest.endpoint</td>
    <td>**path/to/post/resource**/</td>
    <td><b>Mantadory</b>. Rest endpoint <br>where the found notifications should be sent for handling.</td>
  </tr>
  <tr>
    <td>github.auth.token</td>
    <td>string</td>
    <td><b>Mantadory</b>. Github agent's access token. It should only have permissions to check notifications, star repos and
    post comments. <b>Do not give full permissions!</b></td>
  </tr>
  <tr>
    <td>LOG_ROOT</td>
    <td>string</td>
    <td><b>Optional</b>. Place where the log files will be stored. Defaults to . (dot)</td>
  </tr>
</table>
