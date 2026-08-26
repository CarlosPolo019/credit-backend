package com.fya.credits.service.email;

import com.fya.credits.model.EmailJob;

public interface EmailService {
  void sendCreditRegistered(EmailJob job);
}
