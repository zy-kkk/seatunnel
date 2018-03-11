# encoding: utf-8

import smtplib
from email.mime.text import MIMEText
from email.header import Header

from alert_util import _match_alert


class Emails(object):
    def send_alert(self, config, level, subject, objects, content):
        config = config['emails']
        sender = config['sender']
        receivers = config['receivers']
        smtpserver = config['smtp_server']
        username = config['auth_username']
        password = config['auth_password']

        if _match_alert(config['routes'], level):

            message = MIMEText(content, 'text', 'utf-8')
            message['Subject'] = Header(subject + objects, 'utf-8')

            smtp = smtplib.SMTP()
            smtp.connect(smtpserver)
            smtp.login(username, password)
            smtp.sendmail(sender, receivers, message.as_string())

    def check_config(self, config):
        print config
