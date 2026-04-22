package com.example.mcacapitals.capital;

import java.util.UUID;

final class CapitalRecordIdentityState {
    UUID sovereign;
    boolean sovereignFemale;
    UUID consort;
    boolean consortFemale;
    UUID dowager;
    boolean dowagerFemale;
    UUID heir;
    boolean heirFemale;
    CapitalRecord.HeirMode heirMode = CapitalRecord.HeirMode.DYNASTIC;
}