import * as React from "react"
import { cn } from "@/lib/utils"
import { AlertCircle, CheckCircle2, Info, X, AlertTriangle } from "lucide-react"

interface AlertProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'destructive' | 'warning' | 'success'
}

export function Alert({ className, variant = 'default', children, ...props }: AlertProps) {
  const variants = {
    default: "bg-background text-foreground border-border",
    destructive: "border-destructive/50 text-destructive bg-destructive/10 [&>svg]:text-destructive",
    warning: "border-yellow-500/50 text-yellow-700 bg-yellow-50 [&>svg]:text-yellow-600",
    success: "border-emerald-500/50 text-emerald-700 bg-emerald-50 [&>svg]:text-emerald-600",
  }
  return (
    <div
      role="alert"
      className={cn(
        "relative w-full rounded-lg border p-4 [&>svg~*]:pl-7 [&>svg+div]:translate-y-[-3px] [&>svg]:absolute [&>svg]:left-4 [&>svg]:top-4",
        variants[variant],
        className
      )}
      {...props}
    >
      {children}
    </div>
  )
}

export function AlertTitle({ className, ...props }: React.HTMLAttributes<HTMLHeadingElement>) {
  return <h5 className={cn("mb-1 font-medium leading-none tracking-tight", className)} {...props} />
}

export function AlertDescription({ className, ...props }: React.HTMLAttributes<HTMLParagraphElement>) {
  return <div className={cn("text-sm [&_p]:leading-relaxed", className)} {...props} />
}

export function AlertIcon({ variant }: { variant: AlertProps['variant'] }) {
  const Icon = variant === 'destructive' ? AlertCircle : variant === 'warning' ? AlertTriangle : variant === 'success' ? CheckCircle2 : Info
  return <Icon className="h-4 w-4" />
}