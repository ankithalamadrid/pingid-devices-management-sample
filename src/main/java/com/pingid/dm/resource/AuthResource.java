package com.pingid.dm.resource;

import com.pingid.api.resource.GetUserDetailsResource;
import com.pingid.dm.DevicesMgmtMgr;
import com.pingid.dm.config.Config;
import com.pingid.dm.config.Constants;
import com.pingid.dm.crypto.CryptoUtil;
import com.pingid.dm.ppm.PPMResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/auth")
public class AuthResource {

    @POST
    @Path("/start")
    @Produces(MediaType.APPLICATION_JSON)
    public void startReg(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @FormParam("deviceId") String deviceId) throws Exception {

        String userName = (String) request.getSession().getAttribute(Constants.SessionAttribute.USER_NAME);
        request.getSession().setAttribute("deviceId", deviceId);
        DevicesMgmtMgr.INSTANCE.authenticate(userName, deviceId, request, response);
    }

    @POST
    @Path("/finish")
    @Produces(MediaType.APPLICATION_JSON)
    public void finishReg(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @FormParam("ppm_response") String ppmResponse) throws Exception {

        Config config = Config.getInstance(request.getSession().getServletContext());

        String orgKey = config.getProperty(Constants.PropFileParam.USE_BASE_64_KEY);

        String payload = CryptoUtil.verifyJwt(orgKey, ppmResponse);

        PPMResponse ppmRes = PPMResponse.fromJson(payload);

        request.setAttribute(Constants.RequestParameter.SENDER, Constants.Common.DEVICES_MGMT);

        if (ppmRes.getStatus().equalsIgnoreCase("success")) {
            request.setAttribute("deviceId", request.getSession().getAttribute("deviceId"));
            request.setAttribute("authResult", ppmRes.getStatus());
        }
        else {
            request.setAttribute("authErrMsg", ppmRes.getMessage());
        }

        new GetUserDetailsResource().getUserDetails(request, response, ppmRes.getSub());
    }
}