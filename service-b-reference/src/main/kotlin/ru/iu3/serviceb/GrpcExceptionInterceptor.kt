package ru.iu3.serviceb

import io.grpc.ForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status

class GrpcExceptionInterceptor : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val listener = try {
            next.startCall(call, headers)
        } catch (ex: Throwable) {
            handleException(call, ex)
            return object : ServerCall.Listener<ReqT>() {}
        }

        return object : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
            override fun onHalfClose() {
                try {
                    super.onHalfClose()
                } catch (ex: Throwable) {
                    handleException(call, ex)
                }
            }
        }
    }

    private fun <ReqT, RespT> handleException(call: ServerCall<ReqT, RespT>, ex: Throwable) {
        val status = when (ex) {
            is IllegalArgumentException -> Status.INVALID_ARGUMENT.withDescription(ex.message)
            else -> Status.INTERNAL.withDescription(ex.message ?: "Unknown internal error")
        }
        call.close(status.withCause(ex), Metadata())
    }
}